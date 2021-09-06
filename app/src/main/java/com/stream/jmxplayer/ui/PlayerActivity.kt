package com.stream.jmxplayer.ui


import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.stream.jmxplayer.R
import com.stream.jmxplayer.castconnect.CastServer
import com.stream.jmxplayer.casty.Casty
import com.stream.jmxplayer.model.*
import com.stream.jmxplayer.model.PlayerModel.Companion.DIRECT_PUT
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.toaster
import java.io.IOException
import kotlin.math.max
import kotlin.system.exitProcess


class PlayerActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    Player.Listener,
    AnalyticsListener {

    /*
    Life Cycle
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        logger("onCreate", "called")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        setUpOrientation()
        alertDialogLoading = GlobalFunctions.createAlertDialogueLoading(this)
        getDataFromIntent()
        downloaderUtils = DownloaderUtils(this, playerModel)
        resizeUtils = ResizeUtils(this)
        animationUtils = MAnimationUtils(this)
        allFindViewByID()
        setUpDrawer()

        setUpAppBar()
        setUpAppBarTexts(playerModel.title, playerModel.description, playerModel.mLanguage)

        setUpMenuButton()
        setUpPlayerViewControl()
        startCastServer()
        casty = Casty.create(this)
        //Casty.configure("8639B975")
        casty.setUpMediaRouteButton(castButton)
        casty.setOnCastSessionUpdatedListener { castSession ->
            if (castSession != null) {
                logger(
                    "on session updated",
                    castSession.sessionId
                        ?: "nullSid" + " " + castSession.applicationConnectionResult
                )
                logger(
                    "on session updated",
                    castSession.applicationStatus ?: "nullAppStat" + " " + castSession.castDevice
                )
                logger(
                    "on session updated",
                    castSession.category
                        ?: "nullCategory" + " " + castSession.activeInputState + " " + castSession.applicationMetadata
                )

            }
        }
        casty.setOnConnectChangeListener(object : Casty.OnConnectChangeListener {
            override fun onConnected() {
                toaster(this@PlayerActivity, "connected")
                casty.player.loadMediaAndPlay(PlayerUtils.createMediaData(playerModel))

            }

            override fun onDisconnected() {
                toaster(this@PlayerActivity, "disconnected")
                hideSystemUi()
            }
        })

        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong(_playbackPosition, 0)
            currentWindow = savedInstanceState.getInt(_currentWindowIndex)
            autoPlay = savedInstanceState.getBoolean(_autoPlay)
        }

        adMobAdUtils = AdMobAdUtils(this)
    }

    override fun onStart() {
        logger("Came here", "onStart")
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer()
            logger("MediaData", PlayerUtils.createMediaData(playerModel).toString())
        }
    }

    override fun onResume() {
        super.onResume()
        logger("Came here", "onResume")
        // start in pure full screen
        hideSystemUi()
        if (Util.SDK_INT <= 23) {
            initPlayer()
            // startCastServer()
        }
    }

    override fun onPause() {
        super.onPause()
        logger("Came here", "onPause")
        if (Util.SDK_INT <= 23) {
            releasePlayer()
            //stopCastServer()
        }
    }

    override fun onStop() {
        super.onStop()
        logger("Came here", "onStop")
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
        stopCastServer()
    }

    private fun setUpOrientation() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                hideSystemUi()
            }
        }
    }

    private fun allFindViewByID() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        mPlayerView = findViewById(R.id.media_view)
        forwardButton = mPlayerView.findViewById(R.id.exo_ffwd)
        rewindButton = mPlayerView.findViewById(R.id.exo_rew)
        playButton = mPlayerView.findViewById(R.id.exo_play)
        pauseButton = mPlayerView.findViewById(R.id.exo_pause)
        backButton = findViewById(R.id.exo_back)

        playerTitle = findViewById(R.id.textView_title_exo)
        playerDesc = findViewById(R.id.textView_desc_exo)
        playerLang = findViewById(R.id.textView_language_exo)

        menuButton = findViewById(R.id.exo_menu)
        castButton = findViewById(R.id.exo_custom_cast)

    }

    private fun startCastServer() {
        logger("CAST_SERVER" , "Here")
        castServer = CastServer(this)
        try {
            castServer.start()
        } catch (e: IOException) {
            logger("CAST_SERVER", e.localizedMessage)
        }
    }

    private fun stopCastServer() {
        castServer.stop()
    }

    private fun goAction() {
        if (btnClick == "ADM") {
            adActivity(2)
        } else if (btnClick == "IDM") {
            adActivity(1)
        }
    }

    private fun newPlayModel(state: Bundle) {
        errorCount = 0
        playerModel = state.getSerializable(BUNDLE_MODEL) as PlayerModel
        autoPlay = false
        initPlayer()
    }

    private fun workAfterAdActivity(state: Bundle, beforeAd: Boolean) {
        val stateNow = state.getInt(BUNDLE_STATE, 0)
        if (stateNow < 3) downloaderUtils.downloadVideo(stateNow)
        else if (stateNow == 3 && beforeAd) {
            newPlayModel(state)
        } else if (stateNow == 4) {
            mPlayer?.pause()
            val intentNow = PlayerUtils.createViewIntent(playerModel)
            if (intentNow.resolveActivity(packageManager) != null) {
                startActivity(intentNow)
            } else {
                toaster(this, GlobalFunctions.NO_APP_FOUND_PLAY_MESSAGE)
            }
        }
    }

    private fun adActivity(state: Int) {
        val bundle = Bundle()
        bundle.putInt(BUNDLE_STATE, state)
        if (state == 3)
            bundle.putSerializable(BUNDLE_MODEL, playerModel)
        iAdListener = object : IAdListener {
            override fun onAdActivityDone(result: String) {
                alertDialogLoading.dismiss()
                logger("Ad", result)
                workAfterAdActivity(bundle, false)

            }

            override fun onAdLoadingStarted() {
                alertDialogLoading.show()
            }

            override fun onAdLoaded() {
                alertDialogLoading.dismiss()
                adMobAdUtils.showAd()
            }

            override fun onAdError(error: String) {
                alertDialogLoading.dismiss()
                logger("Ad ", error)
                workAfterAdActivity(bundle, false)
            }
        }
        workAfterAdActivity(bundle, true)
        adMobAdUtils.setAdListener(iAdListener)
        adMobAdUtils.loadAd()
    }

    private fun showDownloadDialog() {
        downloaderUtils.showDownloadDialog(this::hideSystemUi, object : IResultListener {
            override fun workResult(result: Any) {
                btnClick = result as String
                goAction()

            }
        })
    }

    /*
    View SetUps
     */

    private fun setUpPlayerViewControl() {

        animationUtils.setMidFocusExoControl(
            forwardButton,
            R.drawable.ic_forward_red, R.drawable.forward
        )
        animationUtils.setMidFocusExoControl(
            rewindButton,
            R.drawable.ic_backward_red, R.drawable.backward
        )
        animationUtils.setMidFocusExoControl(
            playButton,
            R.drawable.ic_baseline_play_circle_red_filled_24,
            R.drawable.ic_baseline_play_circle_filled_24
        )
        animationUtils.setMidFocusExoControl(
            pauseButton,
            R.drawable.ic_baseline_pause_circle_red_filled_24,
            R.drawable.ic_baseline_pause_circle_filled_24
        )

    }

    private fun setUpAppBar() {
        backButton.setOnClickListener {
            finish()
            finishAffinity()
            exitProcess(0)

        }
        backButton.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.startAnimation(animationUtils.animationInLeft)
            } else {
                v.startAnimation(animationUtils.animationOutLeft)
            }
            if (hasFocus) {
                v.setBackgroundColor(Color.GRAY)
            } else {
                v.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun setUpAppBarTexts(title: String, desc: String, lang: String) {
        setTitleText(title, playerTitle)
        playerDesc.text = desc
        playerLang.text = lang
    }

    private fun setUpMenuButton() {
        menuButton.setOnClickListener {
            hideSystemUi()
            drawerLayout.openDrawer(Gravity.RIGHT)

        }
        menuButton.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.startAnimation(animationUtils.animationInRight)
            } else {
                view.startAnimation(animationUtils.animationOutRight)
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
        resizeUtils.changeResize()
        mPlayerView.resizeMode = resizeUtils.setResize()
        mPlayerView.layoutParams = resizeUtils.setAspectRatio(requestedOrientation)
    }

    private fun changeAspectRatio() {
        resizeUtils.changeAspectRatio()
        mPlayerView.resizeMode = resizeUtils.setResize()
        mPlayerView.layoutParams = resizeUtils.setAspectRatio(requestedOrientation)
    }

    /*
    nav setUp
     */
    private fun setUpDrawer() {
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_orientation -> {
                requestedOrientation =
                    if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
            }
            R.id.menu_download_video -> {
                mPlayer?.pause()
                showDownloadDialog()
            }
            R.id.menu_open_with_other_app -> {
                adActivity(4)
            }
            R.id.menu_resize -> {
                changeResize()
                item.title =
                    resources.getString(R.string.resize) + " : " + resizeUtils.resizeMode.valueStr
            }
            R.id.menu_aspect_ratio -> {
                changeAspectRatio()
                item.title =
                    resources.getString(R.string.aspect_ratio) + " : " + resizeUtils.aspectRatio.valueStr
            }
            R.id.menu_custom_stream -> {
                mPlayer?.pause()
                userInputStream()
            }
            R.id.menu_m3u -> {
                toaster(this, "NOT YET DONE")
            }
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
            playerModel = intent.getSerializableExtra(DIRECT_PUT) as PlayerModel
        } else {
            userInputStream()
        }
    }

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
        inErrorState = false
        if (mPlayer == null) {
            mPlayer = PlayerUtils.createPlayer(this)
            mPlayer?.playWhenReady = autoPlay
            mPlayer?.addListener(this)
            mPlayer?.addAnalyticsListener(this)
            mPlayerView.player = mPlayer
        }
        val mediaSource = PlayerUtils.createMediaSource(this, playerModel, errorCount)
        mPlayer?.setMediaSource(mediaSource)
        mPlayer?.prepare()
        if (autoPlay) {
            mPlayer?.play()
        } else {
            mPlayer?.pause()
        }
        setUpAppBarTexts(playerModel.title, playerModel.description, playerModel.mLanguage)
    }

    private fun releasePlayer() {
        if (mPlayer != null) {
            // save the player state before releasing its resources
            playbackPosition = mPlayer?.currentPosition ?: 0L
            currentWindow = mPlayer?.currentWindowIndex ?: 0
            autoPlay = mPlayer?.playWhenReady ?: true
            mPlayer?.release()
            mPlayer = null
        }
    }

    private fun clearResumePosition() {
        currentWindow = C.INDEX_UNSET
        playbackPosition = C.TIME_UNSET
    }

    private fun updateResumePosition() {
        currentWindow = mPlayer?.currentWindowIndex ?: 0
        playbackPosition = max(0, mPlayer?.contentPosition ?: 0L)
    }

    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        logger("Loading", loadEventInfo.uri.toString())
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
        logger(
            TAG,
            "Error Media :" +
                    " " + mPlayer?.currentMediaItem?.toString() +
                    " " + mPlayer?.mediaMetadata?.mediaUri +
                    " " + mPlayer?.mediaMetadata.toString()
        )
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

    private val _autoPlay = "autoplay"
    private val _currentWindowIndex = "current_window_index"
    private val _playbackPosition = "playback_position"

    private lateinit var alertDialogLoading: AlertDialog
    private lateinit var playerModel: PlayerModel
    private lateinit var btnClick: String

    private var autoPlay = true
    private var inErrorState = false
    private var errorCount = 0

    private var mPlayer: SimpleExoPlayer? = null

    private lateinit var casty: Casty

    private var currentWindow = 0
    private var playbackPosition = 0L

    private lateinit var animationUtils: MAnimationUtils
    private lateinit var playerTitle: TextView
    private lateinit var playerDesc: TextView
    private lateinit var playerLang: TextView

    private lateinit var mPlayerView: PlayerView
    private lateinit var backButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var forwardButton: ImageButton
    private lateinit var rewindButton: ImageButton
    private lateinit var playButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var castButton: MediaRouteButton

    lateinit var adMobAdUtils: AdMobAdUtils
    private lateinit var iAdListener: IAdListener

    private lateinit var castServer: CastServer
    private lateinit var downloaderUtils: DownloaderUtils
    private lateinit var resizeUtils: ResizeUtils


    companion object {
        const val BUNDLE_MODEL = "PLAY_MODEL"
        const val BUNDLE_STATE = "STATE_MODEL"
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