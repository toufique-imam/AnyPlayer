package com.stream.jmxplayer.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultRenderersFactory.ExtensionRendererMode
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.model.PlayerModel.Companion.cookieIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.descriptionIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.drmStringIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.headerIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.languageIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.linkIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.mainLinkIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.playerLatinoDomain
import com.stream.jmxplayer.model.PlayerModel.Companion.titleIntent
import com.stream.jmxplayer.model.PlayerModel.Companion.userAgentIntent
import org.json.JSONObject


class PlayerUtils {
    companion object {
         fun createJSONObject(playerModel: PlayerModel): JSONObject {
            return JSONObject(Gson().toJson(playerModel))
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
            val headerNow = ArrayList<String>()
            for ((key, value) in playerModel.headers) {
                headerNow.add(key)
                headerNow.add(value)
            }
            intentNow.putExtra(headerIntent, headerNow.toTypedArray())
            return intentNow
        }

        fun createViewIntent(playerModel: PlayerModel): Intent {
            val intentNow = Intent(Intent.ACTION_VIEW)
            intentNow.setDataAndTypeAndNormalize(
                Uri.parse(playerModel.link),
                "video/* , application/x-mpegURL"
            )
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
                playerModel.mainLink = intent.getStringExtra(mainLinkIntent)!!
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

            if (headerNow != null) {
                var index = 1
                while (index < headerNow.size) {
                    playerModel.headers[headerNow[index - 1]] = headerNow[index]
                    index += 2
                }
            }
            if (playerModel.cookies.isNotEmpty()) {
                playerModel.headers["Cookies"] = playerModel.cookies
                playerModel.headers["cookies"] = playerModel.cookies
                playerModel.headers["Cookie"] = playerModel.cookies
                playerModel.headers["cookie"] = playerModel.cookies
            }
            playerModel.headers["user-agent"] = playerModel.userAgent
            playerModel.headers["User-Agent"] = playerModel.userAgent
            //Log.e("playerUtils", playerModel.toString())
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
            val mediaSourceFactory = createMediaSourceFactory(activity, playerModel, errorCount)
            val uriNow = Uri.parse(playerModel.link)
            val mediaItem = MediaItem.Builder()
                .setUri(uriNow)
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
            val httpDataSourceFactory = createDataSourceFactory(activity, playerModel)
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
                    //.setAllowChunklessPreparation(true)
                }
                C.TYPE_SS -> {
                    return SsMediaSource.Factory(httpDataSourceFactory)


                }
                C.TYPE_OTHER -> {
                    if (playerModel.link.contains(playerLatinoDomain)) {
                        return HlsMediaSource.Factory(httpDataSourceFactory)
                        //.setAllowChunklessPreparation(true)

                    }
                    return if (errorCount % 2 == 0) {
                        ProgressiveMediaSource.Factory(httpDataSourceFactory)

                    } else {
                        HlsMediaSource.Factory(httpDataSourceFactory)
                        //.setAllowChunklessPreparation(true)

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