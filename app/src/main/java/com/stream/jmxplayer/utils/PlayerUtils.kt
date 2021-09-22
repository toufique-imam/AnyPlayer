package com.stream.jmxplayer.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultRenderersFactory.ExtensionRendererMode
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.stream.jmxplayer.castconnect.CAST_SERVER_PORT
import com.stream.jmxplayer.model.MediaData
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.model.PlayerModel.Companion.DIRECT_PUT
import com.stream.jmxplayer.model.PlayerModel.Companion.cookieIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.descriptionIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.drmStringIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.headerIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.imageIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.languageIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.linkIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.mainLinkIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.playerLatinoDomain
import com.stream.jmxplayer.model.PlayerModel.Companion.titleIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.typeIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.userAgentIntent
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import org.json.JSONObject
import java.net.URL


class PlayerUtils {
    companion object {
        private fun getMimeType(link: String): String {
            val uriNow = Uri.parse(link)
            when (Util.inferContentType(uriNow)) {
                C.TYPE_HLS -> {
                    return "application/x-mpegurl"
                }
                C.TYPE_DASH -> {
                    return "application/dash+xml"
                }
                C.TYPE_SS -> {
                    return "video/mp4"
                }
                C.TYPE_OTHER -> {
                    return "application/x-mpegurl"
                }
                else -> {
                    return "video/webm"
                }
            }
        }

        fun createMediaData(playerModel: PlayerModel): MediaData {
            var linkNow = playerModel.link
            if (playerModel.streamType == 2) {
                val ipAddresses = GlobalFunctions.getIPAddress(true)
                try {
                    val baseUrl = URL("http", ipAddresses, CAST_SERVER_PORT, "")
                    val videoUrl = baseUrl.toString() + "/video?id=" + playerModel.id
                    linkNow = videoUrl
                    logger("baseUrl", baseUrl.toString())
                    logger("VideoUrl", videoUrl)
                } catch (e: Exception) {
                    logger("createMediaData2", e.localizedMessage ?: "")
                }
            }
            val builder = MediaData.Builder(linkNow)
                .setContentType(getMimeType(linkNow))
                .setTitle(playerModel.title)

            val configNow = JSONObject()
            configNow.put(languageIntent, playerModel.mLanguage)
            configNow.put(descriptionIntent, playerModel.description)
            configNow.put("LOCAL", false)
            if (playerModel.userAgent.isNotEmpty())
                configNow.put(userAgentIntent, playerModel.userAgent)
            if (playerModel.drmSting.isNotEmpty()) {
                configNow.put(drmStringIntent, playerModel.drmSting)
            }
            if (playerModel.cookies.isNotEmpty()) {
                configNow.put(cookieIntent, playerModel.cookies)
            }
            configNow.put(typeIntent, playerModel.streamType)

            for (key in playerModel.headers.keys) {
                configNow.put(key, playerModel.headers[key])
            }

            if (playerModel.streamType == 1) {
                builder.setStreamType(MediaData.STREAM_TYPE_LIVE)
                builder.setMediaType(MediaData.MEDIA_TYPE_GENERIC)
            } else {
                builder.setStreamType(MediaData.STREAM_TYPE_BUFFERED)
                builder.setMediaType(MediaData.MEDIA_TYPE_MOVIE)
            }
            builder.setExoPlayerConfig(configNow)
            builder.addPhotoUrl(playerModel.image)
            builder.addPhotoUrl(playerModel.image)


            return builder.build()
        }

        private fun addExtraToIntent(playerModel: PlayerModel, intentNow: Intent): Intent {
            intentNow.putExtra(
                userAgentIntent,
                playerModel.userAgent
            )
            if (playerModel.cookies.isNotEmpty()) {
                intentNow.putExtra(cookieIntent, playerModel.cookies)
            }
            intentNow.putExtra(languageIntent, playerModel.mLanguage)
            intentNow.putExtra(titleIntent, playerModel.title)
            intentNow.putExtra(descriptionIntent, playerModel.description)
            intentNow.putExtra(mainLinkIntent, playerModel.mainLink)
            intentNow.putExtra(typeIntent, playerModel.streamType)
            val headerNow = ArrayList<String>()
            for ((key, value) in playerModel.headers) {
                headerNow.add(key)
                headerNow.add(value)
            }
            intentNow.putExtra(headerIntent, headerNow.toTypedArray())
            intentNow.putExtra(DIRECT_PUT, playerModel)
            return intentNow
        }

        fun createViewIntent(playerModel: PlayerModel): Intent {
            val intentNow = Intent(Intent.ACTION_VIEW)
            intentNow.setDataAndTypeAndNormalize(
                Uri.parse(playerModel.link),
                "video/* , application/x-mpegURL"
            )
            intentNow.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intentNow.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return addExtraToIntent(playerModel, intentNow)
        }


        fun parseIntent(intent: Intent): PlayerModel {
            /*
                params for intent
                ======================
                1. Link:String (LINK)
                2. User_agent:String (USER_AGENT)
                3. DRM:String (DRM)
                4. cookies:String (COOKIES)
                5. mainLink:String (MAIN_LINK)
                ======================
                1. title:String (TITLE)
                2. description:String (DESCRIPTION)
                3. mLanguage:String (LANGUAGE)
                ======================
                1. header_source_req:String[] (HEADERS)
                ======================
             */
            val playerModel = PlayerModel()
            if (intent.getStringExtra(linkIntent) != null) {
                playerModel.link = intent.getStringExtra(linkIntent)!!
            } else {
                playerModel.link = intent.data.toString()
            }
            if (intent.getStringExtra(mainLinkIntent) != null) {
                playerModel.mainLink = intent.getStringExtra(mainLinkIntent) ?: playerModel.link
            }
            if (intent.getStringExtra(imageIntent) != null) {
                playerModel.image = intent.getStringExtra(imageIntent)!!
            } else {
                playerModel.image = "https://i.ibb.co/j3g5yrh/main-logo.png"
            }
            if (intent.getStringExtra(userAgentIntent) != null) {
                playerModel.userAgent = intent.getStringExtra(userAgentIntent)!!
            } else {
                playerModel.userAgent = GlobalFunctions.USER_AGENT
            }
            if (intent.getStringExtra(drmStringIntent) != null) {
                playerModel.drmSting = intent.getStringExtra(drmStringIntent)!!
            }
            if (intent.getStringExtra(cookieIntent) != null) {
                playerModel.cookies = intent.getStringExtra(cookieIntent)!!
            }

            playerModel.streamType = intent.getIntExtra(typeIntent, 0)


            if (intent.getStringExtra(titleIntent) != null) {
                playerModel.title = intent.getStringExtra(titleIntent)!!
            }
            if (intent.getStringExtra(descriptionIntent) != null) {
                playerModel.description = intent.getStringExtra(descriptionIntent)!!
            }
            if (intent.getStringExtra(languageIntent) != null) {
                playerModel.mLanguage = intent.getStringExtra(languageIntent)!!
            }
            val headerNow = intent.getStringArrayExtra(headerIntent)
            val hashMap = HashMap<String, String>()
            if (headerNow != null) {
                var index = 1
                while (index < headerNow.size) {
                    hashMap[headerNow[index - 1]] = headerNow[index]
                    index += 2
                }
            }
            if (playerModel.cookies.isNotEmpty()) {
                hashMap["Cookies"] = playerModel.cookies
                //playerModel.headers["cookies"] = playerModel.cookies
                hashMap["Cookie"] = playerModel.cookies
                //playerModel.headers["cookie"] = playerModel.cookies
            }
            //playerModel.headers["user-agent"] = playerModel.userAgent
            hashMap["User-Agent"] = playerModel.userAgent
            playerModel.headers = hashMap
            playerModel.id = PlayerModel.getId(playerModel.link, playerModel.title)
            logger("playerUtils", playerModel.toString())
            return playerModel
        }

        private fun createDataSourceFactory(
            activity: Activity,
            playerModel: PlayerModel
        ): DefaultHttpDataSource.Factory {
            val defaultBandwidthMeter = DefaultBandwidthMeter
                .Builder(activity)
                .setInitialBitrateEstimate(2600000L)
                .build()
            return DefaultHttpDataSource
                .Factory()
                .setUserAgent(playerModel.userAgent)
                .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)
                .setAllowCrossProtocolRedirects(true)
                .setTransferListener(defaultBandwidthMeter)
                .setDefaultRequestProperties(playerModel.headers)
        }


        fun createMediaSource(
            activity: Activity,
            playerModel: PlayerModel,
            errorCount: Int
        ): MediaSource {
            val uriNow = Uri.parse(playerModel.link)
            val mediaItem = MediaItem.Builder()
                .setUri(uriNow)

            val mediaSourceFactory = createMediaSourceFactory(activity, playerModel, errorCount)
            if (playerModel.drmSting.isNotEmpty()) {
                val uUID = Util.getDrmUuid(C.WIDEVINE_UUID.toString())
                mediaItem.setDrmUuid(uUID)
                    .setDrmLicenseUri(playerModel.link)
                    .setDrmMultiSession(true)
            }
            return mediaSourceFactory.createMediaSource(mediaItem.build())
        }

        private fun createMediaSourceFactory(
            activity: Activity,
            playerModel: PlayerModel,
            errorCount: Int
        ): MediaSourceFactory {
            logger("createMediaSource", playerModel.toString())
            if (playerModel.streamType == 2) {
                return ProgressiveMediaSource.Factory(
                    DefaultDataSourceFactory(activity, "ua"),
                    DefaultExtractorsFactory()
                )
            }
            val httpDataSourceFactory = createDataSourceFactory(activity, playerModel)
            logger(
                "httpDataSourceFactory", "" +
                        httpDataSourceFactory.defaultRequestProperties.snapshot
            )


            val uriNow = Uri.parse(playerModel.link)
            when (Util.inferContentType(uriNow)) {
                C.TYPE_DASH -> {
                    return DashMediaSource.Factory(
                        DefaultDashChunkSource.Factory(httpDataSourceFactory),
                        httpDataSourceFactory
                    )
                }
                C.TYPE_HLS -> {
                    return HlsMediaSource.Factory(httpDataSourceFactory)
                        .setAllowChunklessPreparation(true)
                }
                C.TYPE_SS -> {
                    return SsMediaSource.Factory(httpDataSourceFactory)


                }
                C.TYPE_OTHER -> {
                    if (playerModel.link.contains(playerLatinoDomain)) {
                        return HlsMediaSource.Factory(httpDataSourceFactory)
                            .setAllowChunklessPreparation(true)

                    }
                    return if (errorCount % 2 == 0) {
                        ProgressiveMediaSource.Factory(httpDataSourceFactory)

                    } else {
                        HlsMediaSource.Factory(httpDataSourceFactory)
                            .setAllowChunklessPreparation(true)

                    }
                }
                else -> {
                    return ProgressiveMediaSource.Factory(httpDataSourceFactory)

                }
            }
        }

        fun createPlayer(activity: Activity): SimpleExoPlayer {
            val loadControl = DefaultLoadControl()
            val trackSelector = DefaultTrackSelector(activity)
            trackSelector.setParameters(
                trackSelector.parameters.buildUpon()
                    .setMaxVideoSizeSd()
                    .setPreferredTextLanguage("es")
                    .setPreferredAudioLanguage("es")
            )

            return if (Build.VERSION.SDK_INT > 22) {
                SimpleExoPlayer.Builder(activity)
                    .setLoadControl(loadControl)
                    .setTrackSelector(trackSelector)
                    .build()
            } else {
                @ExtensionRendererMode val extensionRendererMode =
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                val renderer = DefaultRenderersFactory(activity)
                    .setExtensionRendererMode(extensionRendererMode)
                SimpleExoPlayer.Builder(activity, renderer)
                    .setLoadControl(loadControl)
                    .setTrackSelector(trackSelector)
                    .build()
            }
        }
    }
}
/*
    private fun userInputStream() {
        if (drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            drawerLayout.closeDrawer(Gravity.RIGHT)
        }
        hideSystemUi()

        val dialogueView: View = layoutInflater.inflate(
            R.layout.custom_dialogue_user_stream, null
        )

        val alertDialogueBuilder =
            AlertDialog.Builder(this).setView(dialogueView)

        alertDialogueBuilder.setTitle("Stream Personal")
        val alertDialog = alertDialogueBuilder.create()
        val title: TextInputEditText =
            dialogueView.findViewById(R.id.text_view_custom_stream_name)
        val link: TextInputEditText =
            dialogueView.findViewById(R.id.text_view_custom_stream_url)
        val userAgent: TextInputEditText =
            dialogueView.findViewById(R.id.text_view_custom_stream_user_agent)

        val accept: MaterialButton = dialogueView.findViewById(R.id.button_stream_confirm)
        val cancel: MaterialButton = dialogueView.findViewById(R.id.button_stream_cancel)

        accept.setOnClickListener {
            if (link.text == null || link.text.toString()
                    .isEmpty() || !Patterns.WEB_URL.matcher(
                    link.text.toString()
                ).matches()
            ) {
                link.error = resources.getString(R.string.enter_stream_link)
                link.requestFocus()
            } else {
                val urlNow = link.text.toString()
                val titleNow: String =
                    if (title.text == null || title.text.toString().isEmpty()) {
                        resources.getString(R.string.app_name)
                    } else {
                        title.text.toString()
                    }
                val userAgentNow: String =
                    if (userAgent.text == null || userAgent.text.toString().isEmpty()) {
                        GlobalFunctions.USER_AGENT
                    } else {
                        userAgent.text.toString()
                    }
                playerModel =
                    PlayerModel(title = titleNow, link = urlNow, userAgent = userAgentNow)
                logger("playerModelUser", playerModel.toString())
                PlayerUtils.createMediaData(playerModel)
                alertDialog.dismiss()
                adActivity(3)
            }
        }

        cancel.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.setOnDismissListener {
            hideSystemUi()
        }

        alertDialog.show()
    }*/