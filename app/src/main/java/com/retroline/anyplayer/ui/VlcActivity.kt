package com.retroline.anyplayer.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.mediarouter.app.MediaRouteButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.javiersantos.piracychecker.PiracyChecker
import com.google.android.material.navigation.NavigationView
import com.retroline.anyplayer.R
import com.retroline.anyplayer.adapter.GalleryAdapter
import com.retroline.anyplayer.adapter.GalleryItemViewHolder
import com.retroline.anyplayer.casty.Casty
import com.retroline.anyplayer.model.MediaPlayerControl
import com.retroline.anyplayer.model.PlayerModel
import com.retroline.anyplayer.ui.IJKPlayerActivity.Companion.FROM_ERROR
import com.retroline.anyplayer.ui.fragment.TracksDialogFragment
import com.retroline.anyplayer.ui.view.IjkVideoView
import com.retroline.anyplayer.ui.view.TableLayoutBinder
import com.retroline.anyplayer.ui.view.VideoControlView
import com.retroline.anyplayer.ui.viewmodel.DatabaseViewModel
import com.retroline.anyplayer.utils.GlobalFunctions
import com.retroline.anyplayer.utils.GlobalFunctions.logger
import com.retroline.anyplayer.utils.GlobalFunctions.toaster
import com.retroline.anyplayer.utils.MAnimationUtils
import com.retroline.anyplayer.utils.MediaFileUtils
import com.retroline.anyplayer.utils.PlayerUtils
import com.retroline.anyplayer.utils.Settings
import com.retroline.anyplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.retroline.anyplayer.utils.createAlertDialogueLoading
import com.retroline.anyplayer.utils.createPlayerDialogue
import com.retroline.anyplayer.utils.isStarted
import com.retroline.anyplayer.utils.round
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout

class VlcActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var alertDialogLoading: AlertDialog
    private lateinit var playerModelNow: PlayerModel

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

    private var playerDialog: AlertDialog? = null

    private val viewModel: DatabaseViewModel by viewModels()

    private var idxNow = 0

    private val tagNow = "VLC_Activity"

    private lateinit var mVideoLayout: VLCVideoLayout
    private lateinit var mediaController: VideoControlView

    private lateinit var mediaNow: Media
    private var mLibVLC: LibVLC? = null
    var mVlcPlayer: MediaPlayer? = null
    private var scaleNow = MediaPlayer.ScaleType.SURFACE_BEST_FIT
    lateinit var mSettings: Settings
    lateinit var tracksDialogFragment: TracksDialogFragment

    var fromError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSettings = Settings(this)
        setTheme(mSettings.themeId)
        setContentView(R.layout.activity_vlc)
        piracyChecker?.destroy()
        alertDialogLoading = createAlertDialogueLoading()

        getDataFromIntent()
        setUpOrientation()
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

        tracksDialogFragment = TracksDialogFragment()
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
        mediaController.setMediaPlayer(null)
    }

    private var piracyChecker: PiracyChecker? = null

    override fun onDestroy() {
        super.onDestroy()
        mVlcPlayer?.release()
        mLibVLC!!.release()
        piracyChecker?.destroy()
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

    private fun showVideoTrack(trackSelectionListener: (Int, TracksDialogFragment.TrackType) -> Unit) {
        if (!isStarted()) return
        if (tracksDialogFragment.isAdded) return
        tracksDialogFragment.arguments = bundleOf()
        tracksDialogFragment.show(
            supportFragmentManager,
            "fragment_video_tracks_vlc"
        )
        tracksDialogFragment.trackSelectionListenerVLC = trackSelectionListener
        tracksDialogFragment.onBindInitiated = {
            mVlcPlayer?.let { it1 -> tracksDialogFragment.onVLCPlayerModelChanged(it1) }
        }
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
            fromError = intent.getBooleanExtra(FROM_ERROR, false)
        } else {
            idxNow = 0
        }
        if (PlayListAll.size > idxNow)
            playerModelNow = PlayListAll[idxNow]
        else if (PlayListAll.isNotEmpty()) {
            playerModelNow = PlayListAll[0]
            idxNow = 0
        }
        PlayerUtils.createMediaData(playerModelNow)
    }

    private fun prevTrack() {
        if (PlayListAll.isNotEmpty()) {
            idxNow--
            idxNow += PlayListAll.size
            idxNow %= PlayListAll.size
        }
        updatePlayerModel()

    }

    private fun nextTrack() {
        if (PlayListAll.isNotEmpty()) {
            idxNow++
            idxNow %= PlayListAll.size
        }
        updatePlayerModel()
    }

    private fun initView() {
        recyclerViewPlayList = findViewById(R.id.recycler_playlist_vlc)
        drawerLayout = findViewById(R.id.drawer_layout_vlc)
        navigationView = findViewById(R.id.nav_view_vlc)
        mVideoLayout = findViewById(R.id.media_view)
        mediaController = VideoControlView(this)

    }

    private fun initButtons() {
        playerTitle = mediaController.mTitleView
        playerDesc = mediaController.mDescriptionView
        playerLang = mediaController.mLanguageView
        backButton = mediaController.backButton
        menuButton = mediaController.findViewById(R.id.vlc_menu)

        forwardButton = mediaController.findViewById(R.id.vlc_forward)
        rewindButton = mediaController.findViewById(R.id.vlc_rew)
        pauseButton = mediaController.findViewById(R.id.vlc_pause)
        nextButton = mediaController.findViewById(R.id.playlistNext)
        previousButton = mediaController.findViewById(R.id.playlistPrev)
        audioTrackSelector = mediaController.findViewById(R.id.vlc_track_selector)
        castButton = mediaController.findViewById(R.id.vlc_custom_cast)


        nextButton.setOnClickListener {
            nextTrack()
        }
        previousButton.setOnClickListener {
            prevTrack()
        }
    }

    private fun setUpPlayerViewControl() {
        animationUtils.setMidFocusExoControl(
            forwardButton,
            R.drawable.ic_fast_forward_red, R.drawable.ic_fast_forward
        )
        animationUtils.setMidFocusExoControl(
            rewindButton,
            R.drawable.ic_rewind_red, R.drawable.ic_rewind
        )

        animationUtils.setMidFocusExoControl(
            audioTrackSelector,
            R.drawable.ic_baseline_video_settings_24_red,
            R.drawable.ic_baseline_video_settings_24
        )
        animationUtils.setMidFocusExoControl(
            nextButton,
            R.drawable.ic_skip_forward_red,
            R.drawable.ic_skip_forward
        )
        animationUtils.setMidFocusExoControl(
            previousButton,
            R.drawable.ic_skip_back_red,
            R.drawable.ic_skip_back
        )
        pauseButton.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                pauseButton.startAnimation(animationUtils.animationInMid)
                pauseButton.setImageResource(
                    if (pauseButton.contentDescription == "play") R.drawable.ic_play_red
                    else R.drawable.ic_pause_red
                )
            } else {
                pauseButton.startAnimation(animationUtils.animationInMid)
                pauseButton.setImageResource(
                    if (pauseButton.contentDescription == "play") R.drawable.ic_play
                    else R.drawable.ic_pause
                )
            }
        }

        audioTrackSelector.setOnClickListener {
            showVideoTrack { trackId, trackType ->
                when (trackType) {
                    TracksDialogFragment.TrackType.AUDIO -> {
                        mVlcPlayer?.audioTrack = trackId
                    }
                    TracksDialogFragment.TrackType.VIDEO -> {
                        mVlcPlayer?.videoTrack = trackId
                    }
                    TracksDialogFragment.TrackType.SPU -> {
                        mVlcPlayer?.spuTrack = trackId
                    }
                }
            }
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
        recyclerViewPlayList.visibility = View.VISIBLE
    }

    private fun initCast() {
        cast = Casty.create(this).withMiniController()
        cast.setUpMediaRouteButton(castButton)

        cast.setOnConnectChangeListener(object : Casty.OnConnectChangeListener {
            override fun onConnected() {
                toaster(this@VlcActivity, "connected")
                cast.player.loadMediaAndPlay(PlayerUtils.createMediaData(playerModelNow))
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
            R.id.menu_change_player -> {
                fromError = false
                if (playerDialog == null)
                    playerDialog =
                        createPlayerDialogue(Settings.PV_PLAYER__AndroidMediaPlayer, this::yesSure)
                playerDialog?.show()
            }
            R.id.menu_media_info -> {
                showMediaInfo()
            }
        }
        return false
    }

    private fun showMediaInfo() {
        val builder = TableLayoutBinder(this)
        builder.appendSection(R.string.mi_player)
        builder.appendRow2(R.string.mi_player, "libVLC")
        builder.appendSection(R.string.mi_media)
        builder.appendRow2(
            R.string.mi_resolution,
            IjkVideoView.buildResolution(
                mVlcPlayer?.currentVideoTrack?.width ?: 0,
                mVlcPlayer?.currentVideoTrack?.height ?: 0,
                mVlcPlayer?.currentVideoTrack?.sarNum ?: 0,
                mVlcPlayer?.currentVideoTrack?.sarDen ?: 0,
            )
        )
        builder.appendRow2(
            R.string.mi_length,
            IjkVideoView.buildTimeMilli(mVlcPlayer?.length ?: 0)
        )

        val mediaNow = mVlcPlayer?.media

        if (mediaNow != null) {
            for (i in 0 until mediaNow.trackCount) {
                val trackNow = mediaNow.getTrack(i)
                val typeStr = when (trackNow.type) {
                    IMedia.Track.Type.Video -> {
                        getString(R.string.TrackType_video)
                    }
                    IMedia.Track.Type.Audio -> getString(R.string.TrackType_audio)
                    IMedia.Track.Type.Text -> getString(R.string.TrackType_subtitle)
                    else -> getString(R.string.TrackType_unknown)
                }
                if (trackNow.id == mVlcPlayer?.currentVideoTrack?.id) {
                    builder.appendSection(
                        getString(R.string.mi_stream_fmt1, i) + " " +
                                getString(R.string.mi__selected_video_track)
                    )
                } else {
                    builder.appendSection(
                        getString(
                            R.string.mi_stream_fmt1,
                            i
                        )
                    )
                }
                builder.appendRow2(R.string.mi_type, typeStr)
                builder.appendRow2(R.string.mi_bit_rate, trackNow.bitrate.toString())
                builder.appendRow2(R.string.mi_codec, trackNow.codec)
                if (trackNow.language != null && !trackNow.language.equals(
                        "und",
                        ignoreCase = true
                    )
                )
                    builder.appendRow2(R.string.mi_language, trackNow.language)
                if (trackNow.type == IMedia.Track.Type.Video) {
                    val videoTrack = trackNow as IMedia.VideoTrack
                    val framerate = videoTrack.frameRateNum / videoTrack.frameRateDen.toDouble()
                    if (videoTrack.width != 0 && videoTrack.height != 0) {
                        builder.appendRow2(
                            R.string.mi_resolution,
                            "${videoTrack.width}x${videoTrack.height}"
                        )
                    }
                    builder.appendRow2(R.string.mi_frame_rate, "${framerate.round()}")
                }
                if (trackNow.type == IMedia.Track.Type.Audio) {
                    val audioTrack = trackNow as IMedia.AudioTrack
                    builder.appendRow2(R.string.mi_channels, audioTrack.channels.toString())
                    builder.appendRow2(R.string.mi_sample_rate, audioTrack.rate.toString())
                }
            }
        }

        val adBuilder = builder.buildAlertDialogBuilder()
        adBuilder.setTitle(R.string.media_information)
        adBuilder.setNegativeButton(R.string.close, null)
        adBuilder.show()
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
            if (playerModelNow.userAgent.isNotEmpty()) {
                mLibVLC?.setUserAgent(
                    GlobalFunctions.getReferer(playerModelNow), playerModelNow.userAgent
                )
            }
            Media(mLibVLC, Uri.parse(playerModelNow.link))
        }
    }

    private fun preparePlayer() {
        addSource()
        audioTrackSelector.isEnabled = false
        //audioTrackSelector.visibility = View.GONE
        scaleNow = MediaPlayer.ScaleType.SURFACE_BEST_FIT
        mVlcPlayer?.stop()
        mVlcPlayer?.play(mediaNow)
        mVlcPlayer?.videoScale = scaleNow
        mediaNow.release()
    }

    private fun yesSure(playerID: Int) {
        if (playerID == Settings.PV_PLAYER__AndroidMediaPlayer) return
        releasePlayer()
        val intent = GlobalFunctions.getIntentPlayer(this, playerID)
        intent.putExtra(PlayerModel.SELECTED_MODEL, idxNow)
        intent.putExtra(FROM_ERROR, fromError)
        finish()
        startActivity(intent)
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
                    logger(tagNow, "Event Error")
                    audioTrackSelector.isEnabled = false
                    //audioTrackSelector.visibility = View.GONE
                    if (!fromError) {
                        fromError = true
                        yesSure(Settings.PV_PLAYER__IjkExoMediaPlayer)
                    } else if (PlayListAll.isNotEmpty() && PlayListAll.size > 1) {
                        fromError = false
                        nextTrack()
                    }
                }
                MediaPlayer.Event.Stopped -> {
                    logger(tagNow, "event stopped")
                }
                MediaPlayer.Event.Playing -> {
                    logger(tagNow, "event playing")
                    audioTrackSelector.isEnabled = true
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