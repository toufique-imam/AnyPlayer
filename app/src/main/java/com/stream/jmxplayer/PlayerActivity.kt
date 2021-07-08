package com.stream.jmxplayer


import android.app.Activity
import android.app.MediaRouteButton
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.stream.jmxplayer.model.EAspectRatio
import com.stream.jmxplayer.model.EResizeMode
import com.stream.jmxplayer.model.IAdListener
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.toaster
import com.stream.jmxplayer.utils.PlayerUtils
import com.stream.jmxplayer.utils.UnityAdUtils
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import kotlin.math.max

class PlayerActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    Player.Listener,
    AnalyticsListener {

    private val _autoPlay = "autoplay"
    private val _currentWindowIndex = "current_window_index"
    private val _playbackPosition = "playback_position"

    private lateinit var alertDialogLoading: AlertDialog

    private lateinit var playerModel: PlayerModel
    private lateinit var downloader: String
    private lateinit var btnClick: String
    private var autoPlay = true
    private var inErrorState = false
    private var errorCount = 0

    private var mPlayer: SimpleExoPlayer? = null

    private var currentWindow = 0
    private var playbackPosition = 0L


    private lateinit var _animationInLeft: Animation
    private lateinit var _animationOutLeft: Animation
    private lateinit var _animationInRight: Animation
    private lateinit var _animationOutRight: Animation
    private lateinit var _animationInMid: Animation
    private lateinit var _animationOutMid: Animation


    private lateinit var playerTitle: TextView
    private lateinit var playerDesc: TextView
    private lateinit var playerLang: TextView
    private lateinit var mPlayerView: PlayerView
    private lateinit var backButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var castButton: MediaRouteButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private var aspectRatio: EAspectRatio = EAspectRatio.ASPECT_MATCH
    private var resizeMode: EResizeMode = EResizeMode.FIT

    lateinit var unityAdUtils: UnityAdUtils
    lateinit var iAdListener: IAdListener

    lateinit var mCastContext: CastContext
    var mCastSession: CastSession? = null
    lateinit var mediaItem: MediaItem

    private var mPlayBackState: PlaybackState = PlaybackState.IDLE
    private var mLocation: PlaybackLocation = PlaybackLocation.LOCAL


    lateinit var mSessionManagerListener: SessionManagerListener<CastSession>

    /**
     * indicates whether we are doing a local or a remote playback
     */
    enum class PlaybackLocation {
        LOCAL, REMOTE
    }

    /**
     * List of various states that we can be in
     */
    enum class PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE
    }
    /*
    Cast Functions
     */

    private fun setupCastListeners() {
        mSessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarting(p0: CastSession) {}

            override fun onSessionStarted(p0: CastSession, p1: String) {
                onApplicationConnected(p0)
            }

            override fun onSessionStartFailed(p0: CastSession, p1: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionEnding(p0: CastSession) {}

            override fun onSessionEnded(p0: CastSession, p1: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionResuming(p0: CastSession, p1: String) {}

            override fun onSessionResumed(p0: CastSession, p1: Boolean) {
                onApplicationConnected(p0)
            }

            override fun onSessionResumeFailed(p0: CastSession, p1: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionSuspended(p0: CastSession, p1: Int) {}

            fun onApplicationConnected(castSession: CastSession) {
                mCastSession = castSession
                if (mPlayBackState == PlaybackState.PLAYING) {
                    mPlayer!!.pause()
                    loadRemoteMedia(playbackPosition, true)
                } else {
                    mPlayBackState = PlaybackState.IDLE

                }
                updatePlayButton(mPlayBackState)
                // supportInvalidateOptionsMenu()

            }

            fun onApplicationDisconnected() {
                updatePlayBackLocation(PlaybackLocation.LOCAL)
                mPlayBackState = PlaybackState.IDLE
                mLocation = PlaybackLocation.LOCAL
                updatePlayButton(mPlayBackState)
                // supportInvalidateOptionsMenu()

            }

        }
    }

    private fun setUpControlsCallbacks() {

    }

    fun updatePlayButton(playbackState: PlaybackState) {

    }

    fun playCastLocal(position: Long) {
        //todo
        //startControllersTimer()
        if (mLocation == PlaybackLocation.LOCAL) {
            mPlayer?.seekTo(position)
            mPlayer?.play()
        } else if (mLocation == PlaybackLocation.REMOTE) {
            mPlayBackState = PlaybackState.BUFFERING
            updatePlayButton(mPlayBackState)
            mCastSession?.remoteMediaClient?.seek(position)
        }
        //todo
        //restartTrickPlayTimer()
    }


    private fun buildMediaInfo(): MediaInfo? {
        val movieData: MediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        movieData.putString(MediaMetadata.KEY_TITLE, playerModel.title)

        return MediaInfo.Builder(playerModel.link)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("videos/*")
            .setMetadata(movieData)
            .setCustomData(PlayerUtils.createJSONObject(playerModel))
            .build()
    }

    fun loadRemoteMedia(position: Long, autoPlay: Boolean) {
        if (mCastSession == null) {
            return
        }
        val mediaClient = mCastSession!!.remoteMediaClient ?: return
        mediaClient.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(buildMediaInfo())
                .setAutoplay(autoPlay)
                .setCurrentTime(position)
                .build()
        )
    }


    fun updatePlayBackLocation(location: PlaybackLocation) {

    }

    /*
    Download and Ads
     */
    private fun downloadWithADM() {
        val appInstalled = GlobalFunctions.appInstalledOrNot(this, "com.dv.adm")
        val appInstalled2 = GlobalFunctions.appInstalledOrNot(this, "com.dv.adm.pay")
        val appInstalled3 = GlobalFunctions.appInstalledOrNot(this, "com.dv.adm.old")

        val str3: String
        if (appInstalled || appInstalled2 || appInstalled3) {
            str3 = when {
                appInstalled2 -> {
                    "com.dv.adm.pay"
                }
                appInstalled -> {
                    "com.dv.adm"
                }
                else -> {
                    "com.dv.adm.old"
                }
            }

            downloadAction(str3)

        } else {
            str3 = "com.dv.adm"
            //prompt to download ADM
            marketAction(str3)
        }
    }

    private fun downloadAction(str3: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(playerModel.link), "application/x-mpegURL")
            intent.`package` = str3
            if (playerModel.cookies.isNotEmpty()) {
                intent.putExtra("Cookie", playerModel.cookies)
                intent.putExtra("cookie", playerModel.cookies)
                intent.putExtra("Cookies", playerModel.cookies)
                intent.putExtra("cookie", playerModel.cookies)
            }
            startActivity(intent)
            return
        } catch (e: Exception) {
            return
        }
    }

    private fun marketAction(str3: String) {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$str3")
                )
            )
        } catch (e: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$str3")
                )
            )
        }
    }

    private fun downloadWithIDM() {
        val appInstalled = GlobalFunctions.appInstalledOrNot(this, "idm.internet.download.manager")
        val str3 = "idm.internet.download.manager"
        if (appInstalled) {
            downloadAction(str3)
        } else {
            marketAction(str3)
        }
    }

    private fun goAction() {
        if (btnClick == "ADM") {
            adActivity(this, 2)
        } else if (btnClick == "IDM") {
            adActivity(this, 1)
        }
    }

    private fun workAfterAdActivity(state: Int) {
        when (state) {
            1 -> {
                downloadWithIDM()
            }
            2 -> {
                downloadWithADM()
            }
            4 -> {
                mPlayer?.pause()
                val intentNow = PlayerUtils.createViewIntent(playerModel)
                intentNow.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intentNow.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                if (intentNow.resolveActivity(packageManager) != null) {
                    startActivity(intentNow)
                } else {
                    toaster(this, GlobalFunctions.NO_APP_FOUND_PLAY_MESSAGE)
                }
            }
            3 -> {
                setUpAppBarTexts()
                initPlayer()
            }
        }
    }

    private fun adActivity(activity: Activity, state: Int) {
        iAdListener = object : IAdListener {
            override fun onAdActivityDone(result: String) {
                alertDialogLoading.dismiss()
                logger("Splash Ad", result)
                workAfterAdActivity(state)

            }

            override fun onAdLoadingStarted() {
                alertDialogLoading.show()
            }

            override fun onAdLoaded() {
                alertDialogLoading.dismiss()
                unityAdUtils.showAd(activity)
            }

            override fun onAdError(error: String) {
                alertDialogLoading.dismiss()
                logger("Splash Ad", error)
                workAfterAdActivity(state)
            }
        }
        unityAdUtils.addListener(iAdListener)
        unityAdUtils.loadAd()
    }

    private fun showDownloadDialog() {
        val dialogueView: View = layoutInflater.inflate(R.layout.custom_dialogue_download, null)
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Descargar con")
        builder.setView(dialogueView)
        val positiveButton: Button = dialogueView.findViewById(R.id.button_exit_ac)
        val negativeButton: Button = dialogueView.findViewById(R.id.button_exit_wa)

        val alertDialog: AlertDialog = builder.create()
        val radioGroup: RadioGroup = dialogueView.findViewById(R.id.radio_group_download)

        downloader = ""

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val radioButtonNow: RadioButton = group.findViewById(checkedId)
            downloader = if (radioButtonNow.text.toString().trim() == "ADM") {
                "ADM"
            } else {
                "IDM"
            }
        }
        positiveButton.setOnClickListener {
            if (downloader.isEmpty()) {
                toaster(this, "Please Select an app to download")
            } else {
                btnClick = downloader
                alertDialog.dismiss()
                goAction()
            }
        }
        negativeButton.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.setOnDismissListener {
            hideSystemUi()
        }

        alertDialog.show()
    }

    /*
    View SetUps
     */

    private fun setFocusOnExoControl(
        view: ImageView,
        animation_in: Animation, animation_out: Animation,
        imageInID: Int, imageOutID: Int
    ) {
        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                view.startAnimation(animation_in)
                view.setImageResource(imageInID)
            } else {
                view.startAnimation(animation_out)
                view.setImageResource(imageOutID)
            }
        }
    }

    private fun setUpPlayerViewControl() {
        val forwardButton: ImageButton = mPlayerView.findViewById(R.id.exo_ffwd)
        val rewindButton: ImageButton = mPlayerView.findViewById(R.id.exo_rew)
        val playButton: ImageButton = mPlayerView.findViewById(R.id.exo_play)
        val pauseButton: ImageButton = mPlayerView.findViewById(R.id.exo_pause)

        setFocusOnExoControl(
            forwardButton,
            _animationInMid, _animationOutMid,
            R.drawable.ic_forward_red, R.drawable.forward
        )
        setFocusOnExoControl(
            rewindButton,
            _animationInMid, _animationOutMid,
            R.drawable.ic_backward_red, R.drawable.backward
        )
        setFocusOnExoControl(
            playButton,
            _animationInMid, _animationOutMid,
            R.drawable.ic_baseline_play_circle_red_filled_24,
            R.drawable.ic_baseline_play_circle_filled_24
        )
        setFocusOnExoControl(
            pauseButton,
            _animationInMid,
            _animationOutMid,
            R.drawable.ic_baseline_pause_circle_red_filled_24,
            R.drawable.ic_baseline_pause_circle_filled_24
        )

    }

    private fun initAnimation() {

        _animationInLeft = AnimationUtils.loadAnimation(this, R.anim.scale_in_left)
        _animationOutLeft = AnimationUtils.loadAnimation(this, R.anim.scale_out_left)
        _animationInLeft.fillAfter = true
        _animationOutLeft.fillAfter = true
        _animationInRight = AnimationUtils.loadAnimation(this, R.anim.scale_in_right)
        _animationOutRight = AnimationUtils.loadAnimation(this, R.anim.scale_out_right)

        _animationInRight.fillAfter = true
        _animationOutRight.fillAfter = true

        _animationInMid = AnimationUtils.loadAnimation(this, R.anim.scale_in_mid)
        _animationOutMid = AnimationUtils.loadAnimation(this, R.anim.scale_out_mid)

        _animationInMid.fillAfter = true
        _animationOutMid.fillAfter = false


    }

    private fun appBarSetup() {
        backButton = findViewById(R.id.exo_back)
        backButton.setOnClickListener {
            finish()
        }
        backButton.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.startAnimation(_animationInLeft)
            } else {
                v.startAnimation(_animationOutLeft)
            }
            if (hasFocus) {
                v.setBackgroundColor(Color.GRAY)
            } else {
                v.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        setUpMenuButton()

        setUpCastButton()

        setUpAppBarTexts()

    }

    private fun setUpAppBarTexts() {
        playerTitle = findViewById(R.id.textView_title_exo)
        setTitleText(playerModel.title, playerTitle)

        playerDesc = findViewById(R.id.textView_desc_exo)
        playerDesc.text = playerModel.description

        playerLang = findViewById(R.id.textView_language_exo)
        playerLang.text = playerModel.mLanguage
    }

    private fun setUpMenuButton() {
        menuButton = findViewById(R.id.exo_menu)
        menuButton.setOnClickListener {
            hideSystemUi()
            drawerLayout.openDrawer(Gravity.RIGHT)

        }
        menuButton.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.startAnimation(_animationInRight)
            } else {
                view.startAnimation(_animationOutRight)
            }
            if (hasFocus) {
                view.setBackgroundColor(Color.GRAY)
            } else {
                view.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun setUpCastButton() {
        castButton = findViewById(R.id.exo_custom_cast)
        castButton.setOnClickListener {
            toaster(this, "cast NOT DONE")
        }
        castButton.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.startAnimation(_animationInRight)
            } else {
                view.startAnimation(_animationOutRight)
            }
            if (hasFocus) {
                view.setBackgroundColor(Color.GRAY)
            } else {
                view.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun setTitleText(text: String, tv: TextView) {
        var textNow = text
        if (this.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            if (text.length > 50) {
                textNow = text.substring(0, 50) + ".."
            }
        } else {
            if (text.length > 30) {
                textNow = text.substring(0, 30) + ".."
            }
        }
        tv.text = textNow
    }

    private fun changeResize() {
        resizeMode = EResizeMode.next(resizeMode.valueStr)
        toaster(this, resizeMode.valueStr)
        setResize()
        setAspectRatio()
    }

    private fun setResize() {
        when (resizeMode) {
            EResizeMode.FILL -> mPlayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            EResizeMode.ZOOM -> mPlayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> mPlayerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    private fun changeAspectRatio() {
        aspectRatio = EAspectRatio.next(aspectRatio.valueStr)
        toaster(this, aspectRatio.valueStr)
        setResize()
        setAspectRatio()
    }

    private fun setAspectRatio() {
        val width = GlobalFunctions.getScreenWidth(this)
        val height = GlobalFunctions.getScreenHeight(this)

        val orientationNow = requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val params: FrameLayout.LayoutParams

        when (aspectRatio) {
            EAspectRatio.ASPECT_1_1 -> {
                params = if (orientationNow) {
                    FrameLayout.LayoutParams(width, width)
                } else {
                    FrameLayout.LayoutParams(height, height)
                }

            }
            EAspectRatio.ASPECT_4_3 -> {
                params = if (orientationNow) {
                    FrameLayout.LayoutParams(width, (3 * width) / 4)
                } else {
                    FrameLayout.LayoutParams((height * 4) / 3, height)
                }
            }
            EAspectRatio.ASPECT_16_9 -> {
                params = if (orientationNow) {
                    FrameLayout.LayoutParams(width, (16 * width) / 9)
                } else {
                    FrameLayout.LayoutParams((height * 9) / 16, height)
                }
            }
            EAspectRatio.ASPECT_MATCH -> {
                params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            else -> {
                params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
        params.gravity = Gravity.CENTER
        mPlayerView.layoutParams = params
    }

    /*
    nav setUp
     */
    private fun setUpDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_change_orientation) {
            requestedOrientation =
                if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
        } else if (id == R.id.menu_download_video) {
            mPlayer?.pause()
            showDownloadDialog()
        } else if (id == R.id.menu_open_with_other_app) {
            adActivity(this, 4)
        } else if (id == R.id.menu_resize) {
            changeResize()
            item.title = resources.getString(R.string.resize) + " : " + resizeMode.valueStr
        } else if (id == R.id.menu_aspect_ratio) {
            changeAspectRatio()
            item.title = resources.getString(R.string.aspect_ratio) + " : " + aspectRatio.valueStr
        } else if (id == R.id.menu_custom_stream) {
            mPlayer?.pause()
            userInputStream()
        } else if (id == R.id.menu_m3u) {
            toaster(this, "NOT YET DONE")
        }
        return true
    }

    /*
    PlayerModel Input / Parsing
     */
    private fun getDataFromIntent() {
        val intent: Intent
        if (getIntent() != null) {
            intent = getIntent()
            playerModel = intent.getSerializableExtra(PlayerModel.DIRECT_PUT) as PlayerModel
        } else {
            userInputStream()
        }
        appBarSetup()
    }

    private fun userInputStream() {

        val dialogueView: View = layoutInflater.inflate(
            R.layout.custom_dialogue_user_stream, null
        )

        val alertDialogueBuilder =
            AlertDialog.Builder(this).setView(dialogueView)

        alertDialogueBuilder.setTitle("Stream Personal")
        val alertDialog = alertDialogueBuilder.create()
        val title: TextInputEditText = dialogueView.findViewById(R.id.text_view_custom_stream_name)
        val link: TextInputEditText = dialogueView.findViewById(R.id.text_view_custom_stream_url)
        val userAgent: TextInputEditText =
            dialogueView.findViewById(R.id.text_view_custom_stream_user_agent)

        val accept: MaterialButton = dialogueView.findViewById(R.id.button_stream_confirm)
        val cancel: MaterialButton = dialogueView.findViewById(R.id.button_stream_cancel)

        accept.setOnClickListener {
            if (link.text == null || link.text.toString().isEmpty() || !Patterns.WEB_URL.matcher(
                    link.text.toString()
                ).matches()
            ) {
                link.error = resources.getString(R.string.enter_stream_link)
                link.requestFocus()
            } else {
                val urlNow = link.text.toString()
                val titleNow: String = if (title.text == null || title.text.toString().isEmpty()) {
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
                playerModel = PlayerModel(titleNow, urlNow, userAgentNow)
                alertDialog.dismiss()
                adActivity(this, 3)
            }
        }

        cancel.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.setOnDismissListener {
            hideSystemUi()
        }

        alertDialog.show()
    }

    /*
    ExoPlayer Functions
     */
    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT < 30) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        if (Build.VERSION.SDK_INT < 30) {
            mPlayerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        } else {
            val controller: WindowInsetsController? = mPlayerView.windowInsetsController
            if (controller != null) {
                controller.hide(
                    (WindowInsets.Type.statusBars()
                            or WindowInsets.Type.navigationBars()
                            or WindowInsets.Type.systemBars())
                )
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun initPlayer() {
        Log.e("MainActivity", "came here")
        if (mPlayer == null) {
            mPlayer = PlayerUtils.createPlayer(this)
            mPlayer!!.playWhenReady = autoPlay
            mPlayer!!.seekTo(currentWindow, playbackPosition)
            mPlayer!!.addListener(this)
            mPlayer!!.addAnalyticsListener(this)
            mPlayerView.player = mPlayer
        }
        setAspectRatio()
        setResize()
        val mediaSource = PlayerUtils.createMediaSource(this, playerModel, errorCount)
        mPlayer!!.setMediaSource(mediaSource)
        mPlayer!!.prepare()
    }

    private fun releasePlayer() {
        if (mPlayer != null) {
            // save the player state before releasing its resources
            playbackPosition = mPlayer!!.currentPosition
            currentWindow = mPlayer!!.currentWindowIndex
            autoPlay = mPlayer!!.playWhenReady
            mPlayer!!.release()
            mPlayer = null
        }
    }

    private fun clearResumePosition() {
        currentWindow = C.INDEX_UNSET
        playbackPosition = C.TIME_UNSET
    }

    private fun updateResumePosition() {
        currentWindow = mPlayer!!.currentWindowIndex
        playbackPosition = max(0, mPlayer!!.contentPosition)
    }


    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        if (loadEventInfo.uri.toString() == MAGNA_TV_BLOCKED) {
            playerModel.link = ""
            inErrorState = true
            releasePlayer()
            toaster(this, SERVER_ERROR_TEXT)
        }
    }

    override fun onPlayerStateChanged(
        eventTime: AnalyticsListener.EventTime,
        playWhenReady: Boolean,
        playbackState: Int
    ) {
        val stateString: String
        when (playbackState) {
            ExoPlayer.STATE_IDLE -> {
                stateString = "STATE_IDLE"
            }
            ExoPlayer.STATE_BUFFERING -> {
                stateString = "STATE_BUFFERING"
            }
            ExoPlayer.STATE_READY -> {
                stateString = "STATE_READY"
            }
            ExoPlayer.STATE_ENDED -> {
                stateString = "STATE_ENDED"
            }
            else -> {
                stateString = "UNKNOWN"
            }
        }
        logger(TAG, "changed state to $stateString")
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        logger(TAG, "Error : " + error.message)
        logger(TAG, "Error Cause : " + error.cause)
        inErrorState = true
        errorCount++
        if (errorCount == 1) {
            inErrorState = false
            autoPlay = true
            clearResumePosition()
            initPlayer()
            return
        }
        if (isRendererError(error)) {
            inErrorState = false
            clearResumePosition()
            initPlayer()
            return
        }
        toaster(this, SERVER_ERROR_TEXT + " " + error.type)
        if (isBehindLiveWindow(error)) {
            clearResumePosition()
            initPlayer()
        } else {
            updateResumePosition()
        }
    }

    override fun onPositionDiscontinuity(reason: Int) {
        if (inErrorState) updateResumePosition()
    }

    /*
    Life Cycle
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setUpDrawer()

        alertDialogLoading = GlobalFunctions.createAlertDialogueLoading(this)
        setUpControlsCallbacks()
        setUpCastButton()
        mCastContext = CastContext.getSharedInstance(this)

        mCastSession = mCastContext.sessionManager.currentCastSession


        if (mCastSession != null && mCastSession!!.isConnected) {
            updatePlayBackLocation(PlaybackLocation.REMOTE)
        } else {
            updatePlayBackLocation(PlaybackLocation.LOCAL)
        }
        mPlayBackState = PlaybackState.IDLE
        updatePlayButton(mPlayBackState)

        if (Build.VERSION.SDK_INT < 30) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        mPlayerView = findViewById(R.id.media_view)

        getDataFromIntent()
        initAnimation()
        setUpPlayerViewControl()
        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong(_playbackPosition, 0)
            currentWindow = savedInstanceState.getInt(_currentWindowIndex)
            autoPlay = savedInstanceState.getBoolean(_autoPlay)
        }

        unityAdUtils = UnityAdUtils(this, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                logger("unity init", "success")
            }

            override fun onInitializationFailed(
                p0: UnityAds.UnityAdsInitializationError?,
                p1: String?
            ) {
                logger("Unity Init error", p0.toString() + " " + p1)
            }

        })
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        // start in pure full screen
        hideSystemUi()
        if (Util.SDK_INT <= 23 || mPlayer == null) {
            initPlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setTitleText(playerModel.title, playerTitle)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mPlayer != null) {
            outState.putLong(_playbackPosition, playbackPosition)
            outState.putInt(_currentWindowIndex, currentWindow)
            outState.putBoolean(_autoPlay, autoPlay)
        }
    }

    companion object {
        const val SERVER_ERROR_TEXT =
            "Lo sentimos, canal no disponible utilice otro servidor para volver a intentar o vuelva más tarde"
        const val TAG = "ExoPlayer"
        const val MAGNA_TV_BLOCKED = "http://51.158.167.219/con/999_cna2.ts"
        fun isRendererError(exception: ExoPlaybackException): Boolean {
            if (exception.type == ExoPlaybackException.TYPE_RENDERER) {
                return true
            }
            return false
        }

        fun isBehindLiveWindow(exception: ExoPlaybackException): Boolean {
            if (exception.type != ExoPlaybackException.TYPE_SOURCE) {
                return false
            }
            var cause: Throwable? = exception.sourceException
            while (cause != null) {
                if (cause is BehindLiveWindowException) {
                    return true
                }
                cause = cause.cause
            }
            return false
        }
    }
}

/*
archive

private fun buildMediaSource(uri: Uri): MediaSource {
    val userAgent = GlobalFunctions.USER_AGENT
    val httpDataSource = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent)
    val reqProp = HashMap<String, String>()
    if (uri.toString().contains("drive")) {
        reqProp.put("cookie", _playerModel.cookies)
    } else {
        if (xModel.referer != null) {
            reqProp.put("Referer", xModel.referer)
        }
    }
    httpDataSource.setDefaultRequestProperties(_playerModel.headers)
    return buildMediaSource(uri, httpDataSource)
}
private fun buildMediaSource(
    uri: Uri,
    httpDataSource: DefaultHttpDataSource.Factory
): MediaSource {
    val mediaItem = MediaItem.fromUri(uri)
    @C.ContentType val type = Util.inferContentType(uri)
    return when (type) {
        C.TYPE_DASH -> DashMediaSource.Factory(httpDataSource)
            .createMediaSource(mediaItem)
        C.TYPE_SS -> SsMediaSource.Factory(httpDataSource).createMediaSource(mediaItem)
        C.TYPE_HLS -> HlsMediaSource.Factory(httpDataSource).createMediaSource(mediaItem)
        C.TYPE_OTHER -> {
            ProgressiveMediaSource.Factory(httpDataSource).createMediaSource(mediaItem)
        }
        else -> {
            ProgressiveMediaSource.Factory(httpDataSource).createMediaSource(mediaItem)
        }
    }
}

*/