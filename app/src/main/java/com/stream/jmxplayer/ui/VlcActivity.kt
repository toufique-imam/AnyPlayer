package com.stream.jmxplayer.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.mediarouter.app.MediaRouteButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.adapter.GalleryItemViewHolder
import com.stream.jmxplayer.casty.Casty
import com.stream.jmxplayer.model.IAdListener
import com.stream.jmxplayer.model.IResultListener
import com.stream.jmxplayer.model.MediaPlayerControl
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.view.VideoControlView
import com.stream.jmxplayer.ui.viewmodel.DatabaseViewModel
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.toaster
import com.stream.jmxplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class VlcActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var alertDialogLoading: AlertDialog
    private lateinit var playerModelNow: PlayerModel

    //private var playList = ArrayList<PlayerModel>()
    private lateinit var btnClick: String

    private lateinit var cast: Casty

    private lateinit var animationUtils: MAnimationUtils
    private lateinit var playerTitle: TextView
    private lateinit var playerDesc: TextView
    private lateinit var playerLang: TextView

    private lateinit var backButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var forwardButton: ImageButton
    private lateinit var rewindButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var audioTrackSelector: ImageButton

    private lateinit var castButton: MediaRouteButton

    private lateinit var recyclerViewPlayList: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter

    lateinit var adMobAdUtils: AdMobAdUtils
    private lateinit var iAdListener: IAdListener

    private lateinit var downloaderUtils: DownloaderUtils

    private var trackDialog: AlertDialog? = null

    private val viewModel: DatabaseViewModel by viewModels()

    private var idxNow = 0

    private val tagNow = "VLC_Activity"

    private lateinit var mVideoLayout: VLCVideoLayout
    private lateinit var mediaController: VideoControlView

    private lateinit var mediaNow: Media
    private var mLibVLC: LibVLC? = null
    var mVlcPlayer: MediaPlayer? = null
    private var scaleNow = MediaPlayer.ScaleType.SURFACE_BEST_FIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(SharedPreferenceUtils.getTheme(this))
        setContentView(R.layout.activity_vlc)
        alertDialogLoading = GlobalFunctions.createAlertDialogueLoading(this)

        getDataFromIntent()
        setUpOrientation()

        downloaderUtils = DownloaderUtils(this, playerModelNow)
        animationUtils = MAnimationUtils(this)

        initView()
        initPlayer()

        mediaController.setMediaPlayer(playerInterface)
        mediaController.setAnchorView(mVideoLayout)
        initButtons()
        mVideoLayout.setOnClickListener {
            mediaController.show(10000)
            if (recyclerViewPlayList.visibility == View.VISIBLE) recyclerViewPlayList.visibility =
                View.GONE
        }

        navigationView.setNavigationItemSelectedListener(this)

        setUpAppBar()
        updateTexts()

        initCast()
        setUpMenuButton()
        initPlaylist()
        setUpPlayerViewControl()


        adMobAdUtils = AdMobAdUtils(this)

    }

    override fun onStart() {
        super.onStart()
        mVlcPlayer?.attachViews(mVideoLayout, null, true, false)
        addSource()
        preparePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        mVlcPlayer?.release()
        mLibVLC!!.release()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateTexts()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(PlayerModel.DIRECT_PUT, playerModelNow)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        playerModelNow = savedInstanceState.getSerializable(PlayerModel.DIRECT_PUT) as PlayerModel
        updateTexts()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mediaController.setMediaPlayer(null)
    }

    private fun getDataFromIntent() {
        val intent: Intent
        if (getIntent() != null) {
            intent = getIntent()
            idxNow = intent.getIntExtra(PlayerModel.SELECTED_MODEL, 0)
        } else {
            idxNow = 0
        }
        if (PlayListAll.size > idxNow)
            playerModelNow = PlayListAll[idxNow]
        else if (PlayListAll.isNotEmpty()) {
            playerModelNow = PlayListAll[0]
            idxNow = 0
        }
    }

    private fun initView() {
        recyclerViewPlayList = findViewById(R.id.recycler_playlist_vlc)
        drawerLayout = findViewById(R.id.drawer_layout_vlc)
        navigationView = findViewById(R.id.nav_view_vlc)
        mVideoLayout = findViewById(R.id.media_view)
        mediaController = VideoControlView(this)

        mediaController.setPrevNextListeners({
            idxNow++
            if (PlayListAll.isNotEmpty())
                idxNow %= PlayListAll.size
            updatePlayerModel()
        }, {
            idxNow--
            if (PlayListAll.isNotEmpty()) {
                idxNow += PlayListAll.size
                idxNow %= PlayListAll.size
            }
            updatePlayerModel()
        })

    }

    private fun initButtons() {
        playerTitle = mediaController.mTitleView
        playerDesc = mediaController.mDescriptionView
        playerLang = mediaController.mLanguageView
        backButton = mediaController.backButton
        menuButton = mediaController.menuButton

        forwardButton = mediaController.mFfwdButton
        rewindButton = mediaController.mRewButton
        pauseButton = mediaController.mPauseButton
        nextButton = mediaController.mNextButton
        previousButton = mediaController.mPrevButton
        audioTrackSelector = mediaController.mTrackSelectorButton
        castButton = mediaController.mCastButton
    }

    private fun initTrackDialogue() {
        if (mVlcPlayer?.audioTracks == null) return
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_tracks, null)

        val builder = AlertDialog.Builder(this).setTitle("Select Tracks").setView(dialogView)
        builder.setCancelable(true)
        val radioGroup: RadioGroup = dialogView.findViewById(R.id.radio_group_server)
        var cnt = 0
        trackDialog = builder.create()

        for (i in mVlcPlayer?.audioTracks!!) {
            logger("tracks", "${i.id} ${i.name}")
            val radioButton = RadioButton(this)
            radioButton.id = cnt
            radioButton.text = i.name
            radioButton.textSize = 15f
            radioButton.setTextColor(Color.BLACK)
            if (cnt == 0) {
                radioButton.isChecked = true
            }
            radioGroup.addView(radioButton)
            cnt++
        }
        if (cnt == 0)
            audioTrackSelector.visibility = View.GONE
        val materialButton: MaterialButton = dialogView.findViewById(R.id.button_stream_now)

        materialButton.setOnClickListener {
            trackDialog?.dismiss()
            val checkedId = radioGroup.checkedRadioButtonId
            toaster(this, "radio checked $checkedId")
            if (mVlcPlayer?.audioTracks?.isNotEmpty() == true) {
                val temp = mVlcPlayer!!.audioTracks[checkedId]
                logger("trackSelected", temp.name + " " + temp.id)
                mVlcPlayer!!.audioTrack = temp.id
            }
        }
    }

    private fun setUpPlayerViewControl() {
        animationUtils.setMidFocusExoControl(
            forwardButton,
            R.drawable.ic_forward_red, R.drawable.forward
        )
        animationUtils.setMidFocusExoControl(
            rewindButton,
            R.drawable.ic_backward_red, R.drawable.backward
        )
        pauseButton.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                pauseButton.startAnimation(animationUtils.animationInMid)
                pauseButton.setImageResource(
                    if (pauseButton.contentDescription == "play") R.drawable.ic_baseline_play_circle_red_filled_24
                    else R.drawable.ic_baseline_pause_circle_red_filled_24
                )
            } else {
                pauseButton.startAnimation(animationUtils.animationInMid)
                pauseButton.setImageResource(
                    if (pauseButton.contentDescription == "play") R.drawable.ic_baseline_play_circle_filled_24
                    else R.drawable.ic_baseline_pause_circle_filled_24
                )
            }
        }

        audioTrackSelector.setOnClickListener {
            if (trackDialog == null) {
                initTrackDialogue()
            }
            if (trackDialog != null)
                trackDialog?.show()
        }
    }

    private fun initPlaylist() {
        galleryAdapter = GalleryAdapter(
            GalleryItemViewHolder.SINGLE_NO_DELETE,
            { _, pos ->
                idxNow = pos
                updatePlayerModel()
                recyclerViewPlayList.visibility = View.GONE
            }, { _, _ ->

            }
        )
        galleryAdapter.setHasStableIds(true)
        recyclerViewPlayList.also { viewR ->
            viewR.layoutManager = GridLayoutManager(this, 1)
            viewR.adapter = galleryAdapter
        }
        galleryAdapter.updateData(PlayListAll)
    }

    private fun initCast() {
        cast = Casty.create(this).withMiniController()
        cast.setUpMediaRouteButton(castButton)
        cast.setOnCastSessionUpdatedListener { castSession ->
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
        cast.setOnConnectChangeListener(object : Casty.OnConnectChangeListener {
            override fun onConnected() {
                toaster(this@VlcActivity, "connected")
                cast.player.loadMediaAndPlayInBackground(PlayerUtils.createMediaData(playerModelNow))
            }

            override fun onDisconnected() {
                toaster(this@VlcActivity, "disconnected")
                hideSystemUi()
            }
        })
    }

    private fun updateTexts() {
        setUpAppBarTexts(
            playerModelNow.title,
            playerModelNow.description, playerModelNow.mLanguage
        )
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

    private fun setUpAppBarTexts(title: String, desc: String, lang: String) {
        setTitleText(title, playerTitle)
        playerDesc.text = desc
        playerLang.text = lang
    }

    private fun setUpAppBar() {
        backButton.setOnClickListener {
            onBackPressed()
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

    private fun nextScaleType() {
        when (scaleNow) {
            MediaPlayer.ScaleType.SURFACE_BEST_FIT -> {
                scaleNow = MediaPlayer.ScaleType.SURFACE_FILL
            }
            MediaPlayer.ScaleType.SURFACE_FILL -> {
                scaleNow = MediaPlayer.ScaleType.SURFACE_16_9
            }
            MediaPlayer.ScaleType.SURFACE_16_9 -> {
                scaleNow = MediaPlayer.ScaleType.SURFACE_4_3
            }
            MediaPlayer.ScaleType.SURFACE_4_3 -> {
                scaleNow = MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
            }
            MediaPlayer.ScaleType.SURFACE_FIT_SCREEN -> {
                scaleNow = MediaPlayer.ScaleType.SURFACE_ORIGINAL
            }
            else -> scaleNow = MediaPlayer.ScaleType.SURFACE_BEST_FIT
        }
    }

    private fun changeResize() {
        nextScaleType()
        mVlcPlayer?.videoScale = scaleNow
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
        if (playerModelNow.streamType != PlayerModel.STREAM_OFFLINE_AUDIO)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                hideSystemUi()
            }
        }
    }

    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT < 30) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(
                    WindowInsets.Type.statusBars()
                            or WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        if (Build.VERSION.SDK_INT < 30) {
            mVideoLayout.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        } else {
            val controller = mVideoLayout.windowInsetsController
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

    //drawer functions
    private fun showDownloadDialog() {
        downloaderUtils.showDownloadDialog(this::hideSystemUi, object : IResultListener {
            override fun workResult(result: Any) {
                btnClick = result as String
                goAction()
            }
        })
    }

    private fun setUpMenuButton() {
        menuButton.setOnClickListener {
            hideSystemUi()
            drawerLayout.openDrawer(GravityCompat.END)

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

    private fun goAction() {
        if (btnClick == "ADM") {
            adActivity(2)
        } else if (btnClick == "IDM") {
            adActivity(1)
        }
    }

    private fun workAfterAdActivity(state: Int) {
        if (state < 3) downloaderUtils.downloadVideo(state)
        else if (state == 4) {
            mVlcPlayer?.pause()
            val intentNow = PlayerUtils.createViewIntent(playerModelNow)
            if (intentNow.resolveActivity(packageManager) != null) {
                startActivity(intentNow)
            } else {
                toaster(this, GlobalFunctions.NO_APP_FOUND_PLAY_MESSAGE)
            }
        }
    }

    private fun adActivity(state: Int) {
        iAdListener = object : IAdListener {
            override fun onAdActivityDone(result: String) {
                alertDialogLoading.dismiss()
                logger("Ad", result)
                workAfterAdActivity(state)
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
                workAfterAdActivity(state)
            }
        }
        adMobAdUtils.setAdListener(iAdListener)
        adMobAdUtils.loadAd()
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
                mVlcPlayer?.pause()
                showDownloadDialog()
            }
            R.id.menu_open_with_other_app -> {
                adActivity(4)
            }
            R.id.menu_resize -> {
                item.title =
                    resources.getString(R.string.resize) + " : " + scaleNow.name
                changeResize()
            }
            R.id.menu_playlist -> {
                if (recyclerViewPlayList.visibility == View.VISIBLE) {
                    recyclerViewPlayList.visibility = View.GONE
                } else {
                    recyclerViewPlayList.visibility = View.VISIBLE
                }
            }

        }
        return false
    }

    private fun addToHistory(playerModel: PlayerModel) {
        viewModel.insertModel(playerModel)
    }

    private fun updatePlayerModel() {
        playerModelNow = PlayListAll[idxNow]
        updateTexts()
        if (cast.isConnected) {
            cast.player.loadMediaAndPlay(PlayerUtils.createMediaData(playerModelNow))
        }
        // addSource()
        preparePlayer()
    }

    private fun releasePlayer() {
        mVlcPlayer?.stop()
        mVlcPlayer?.detachViews()
    }

    private fun addSource() {
        addToHistory(playerModelNow)
        mediaNow = if (PlayerModel.isLocal(playerModelNow.streamType)) {
            Media(
                mLibVLC,
                MediaFileUtils.getRealPathFromURI(
                    this,
                    Uri.parse(playerModelNow.link),
                    playerModelNow.streamType
                )
            )
        } else {
            Media(mLibVLC, Uri.parse(playerModelNow.link))
        }
    }

    private fun preparePlayer() {
        if (mediaNow.isReleased) {
            addSource()
        }
        trackDialog = null
        audioTrackSelector.visibility = View.GONE
        scaleNow = MediaPlayer.ScaleType.SURFACE_BEST_FIT
        mVlcPlayer?.media = mediaNow
        mVlcPlayer?.videoScale = scaleNow
        mediaNow.release()
        mVlcPlayer?.play()
    }

    private fun initPlayer() {
        val options = GlobalFunctions.getVLCOptions(this)
        mLibVLC = LibVLC(this, options)
        mVlcPlayer = MediaPlayer(mLibVLC)
        mVlcPlayer?.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Opening -> logger(tagNow, "Event Opening")
                MediaPlayer.Event.Buffering -> {
                    logger(tagNow, "Event Buffering")
                }
                MediaPlayer.Event.EncounteredError -> {
                    audioTrackSelector.visibility = View.GONE
                    logger(tagNow, "Event Error")
                }
                MediaPlayer.Event.Stopped -> {
                    logger(tagNow, "event stopped")
                }
                MediaPlayer.Event.Playing -> {
                    logger(tagNow, "event playing")
                    audioTrackSelector.visibility = View.VISIBLE
                }
            }
        }
    }

    private val playerInterface = object : MediaPlayerControl {
        override fun start() {
            if (mVlcPlayer?.hasMedia() == true) {
                mVlcPlayer!!.play()
            }
        }

        override fun pause() {
            mVlcPlayer?.pause()
        }

        override fun getDuration(): Int {
            return (mVlcPlayer?.length ?: 0).toInt()
        }

        override fun getCurrentPosition(): Int {
            return ((mVlcPlayer?.position ?: 0f) * duration).toInt()
        }


        override fun seekTo(pos: Int) {
            logger("seek", "$pos")
            val seek = (pos * 1f) / (duration * 1f)
            mVlcPlayer?.position = seek
        }

        override fun isPlaying(): Boolean {
            return mVlcPlayer?.isPlaying == true
        }

        override fun getBufferPercentage(): Int {
            return 0
        }


        override fun canPause(): Boolean {
            return true
        }

        override fun canSeekBackward(): Boolean {
            return mVlcPlayer?.isSeekable == true
        }

        override fun canSeekForward(): Boolean {
            return mVlcPlayer?.isSeekable == true
        }

        override fun getAudioSessionId(): Int {
            return mVlcPlayer!!.audioTrack
        }
    }

}