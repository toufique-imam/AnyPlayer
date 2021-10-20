package com.stream.jmxplayer.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
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
import com.stream.jmxplayer.model.IRenderView
import com.stream.jmxplayer.model.IResultListener
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.view.IjkVideoView
import com.stream.jmxplayer.ui.view.MeasureHelper
import com.stream.jmxplayer.ui.view.VideoControlView
import com.stream.jmxplayer.ui.viewmodel.DatabaseViewModel
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.stream.jmxplayer.utils.ijkplayer.Settings
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import tv.danmaku.ijk.media.player.misc.ITrackInfo
import java.util.*

class IJKPlayerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    var ijkVideoView: IjkVideoView? = null
    var playerModelNow = PlayerModel(-1)
    private lateinit var alertDialogLoading: AlertDialog
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

    lateinit var mNextButton: ImageButton
    lateinit var mPrevButton: ImageButton

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
    private val tagNow = "IJK_Activity"
    private lateinit var mediaController: VideoControlView

    lateinit var mSettings: Settings
    //lateinit var mHudView: TableLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(SharedPreferenceUtils.getTheme(this))
        setContentView(R.layout.activity_ijkplayer)

        adMobAdUtils = AdMobAdUtils(this)
        mSettings = Settings(this)
        alertDialogLoading = GlobalFunctions.createAlertDialogueLoading(this)
        getDataFromIntent()
        initView()

        //mediaController.setMediaPlayer()
        setUpOrientation()
        downloaderUtils = DownloaderUtils(this, playerModelNow)
        animationUtils = MAnimationUtils(this)

        initButtons()

        navigationView.setNavigationItemSelectedListener(this)

        setUpAppBar()
        updateTexts()

        initCast()
        setUpMenuButton()
        initPlaylist()
        setUpPlayerViewControl()
        initPlayer()
        ijkVideoView?.setOnPreparedListener { audioTrackSelector.visibility = View.VISIBLE }
        ijkVideoView?.setOnCompletionListener {
            audioTrackSelector.visibility = View.GONE
            if (PlayListAll.isNotEmpty() && PlayListAll.size > 1) {
                nextTrack()
            }
        }
        ijkVideoView?.setOnToggleListener(object : IResultListener {
            override fun workResult(result: Any) {
                val res = result as String
                logger("onToggle", res)
                if (res == "Show") {
                    recyclerViewPlayList.visibility = View.GONE
                } else {
                    recyclerViewPlayList.visibility = View.VISIBLE
                }
            }

        })
    }

    private fun initPlayer() {
        IjkMediaPlayer.loadLibrariesOnce(null)
        IjkMediaPlayer.native_profileBegin("libijkplayer.so")
    }

    override fun onStart() {
        super.onStart()
        preparePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
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

    private fun addSource() {
        viewModel.insertModel(playerModelNow)
        ijkVideoView?.setVideoPath(
            if (PlayerModel.isLocal(playerModelNow.streamType))
                MediaFileUtils.getRealPathFromURI(
                    this,
                    Uri.parse(playerModelNow.link),
                    playerModelNow.streamType
                )
            else playerModelNow.link, playerModelNow.headers
        )
    }

    private fun preparePlayer() {
        addSource()
        ijkVideoView?.start()

    }

    private fun releasePlayer() {
        if (ijkVideoView?.isBackgroundPlayEnabled == true)
            ijkVideoView?.enterBackground()
        else {
            ijkVideoView?.stopPlayback()
            ijkVideoView?.release(true)
            ijkVideoView?.stopBackgroundPlay()
        }
        IjkMediaPlayer.native_profileEnd()
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
        mNextButton.setOnClickListener {
            nextTrack()
        }
        mPrevButton.setOnClickListener {
            prevTrack()
        }
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

    private fun setUpMenuButton() {
        menuButton.setOnClickListener {
            logger("IJKPlayer", "menuButton")
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

    private fun initCast() {
        cast = Casty.create(this).withMiniController()
        cast.setUpMediaRouteButton(castButton)

        cast.setOnConnectChangeListener(object : Casty.OnConnectChangeListener {
            override fun onConnected() {
                GlobalFunctions.toaster(this@IJKPlayerActivity, "connected")
                cast.player.loadMediaAndPlayInBackground(PlayerUtils.createMediaData(playerModelNow))
            }

            override fun onDisconnected() {
                GlobalFunctions.toaster(this@IJKPlayerActivity, "disconnected")
                hideSystemUi()
            }
        })
    }

    private fun updatePlayerModel() {
        trackDialog = null
        ijkVideoView?.stopPlayback()
        audioTrackSelector.visibility = View.GONE
        playerModelNow = PlayListAll[idxNow]
        logger("updatePlayerModel", playerModelNow.toString())
        updateTexts()
        if (cast.isConnected) {
            cast.player.loadMediaAndPlay(PlayerUtils.createMediaData(playerModelNow))
        }
        // addSource()
        preparePlayer()
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
        recyclerViewPlayList.visibility = View.GONE
    }

    private fun initTrackDialogue() {
        if (ijkVideoView?.trackInfo == null) return
        if (ijkVideoView?.trackInfo?.size == 0) return
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_tracks, null)

        val builder = AlertDialog.Builder(this).setTitle("Select Tracks").setView(dialogView)
        builder.setCancelable(true)
        val radioGroup: RadioGroup = dialogView.findViewById(R.id.radio_group_server)
        trackDialog = builder.create()
        var cnt = 0
        for ((idx, i) in ijkVideoView?.trackInfo!!.withIndex()) {
            val mInfoInline = String.format(Locale.US, "# %s %d: %s", i.language, idx, i.infoInline)
            logger(
                "track",
                "$idx ${i.trackType}  ${i.infoInline} ${i.language} $mInfoInline"
            )
            if (i.trackType != ITrackInfo.MEDIA_TRACK_TYPE_AUDIO) continue
            val radioButton = RadioButton(this)
            radioButton.id = idx
            radioButton.text = mInfoInline
            radioButton.textSize = 15f
            radioButton.setTextColor(Color.BLACK)
            if (ijkVideoView?.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO) == idx) {
                logger(
                    "selected track",
                    "$idx ${i.trackType}  ${i.infoInline} ${i.language}"
                )
                radioButton.isChecked = true
            }
            radioGroup.addView(radioButton)
            cnt++
        }

        val materialButton: MaterialButton = dialogView.findViewById(R.id.button_stream_now)

        materialButton.setOnClickListener {
            trackDialog?.dismiss()
            val checkedId = radioGroup.checkedRadioButtonId
            GlobalFunctions.toaster(this, "radio checked $checkedId")
            if (!ijkVideoView?.trackInfo.isNullOrEmpty()) {
                val temp = ijkVideoView?.trackInfo!![checkedId]

                val currentTrack = ijkVideoView?.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
                if (currentTrack != null) {
                    ijkVideoView?.deselectTrack(currentTrack)
                }
                logger(
                    "checked_tracks",
                    "${temp.trackType} ${temp.infoInline} ${temp.language}"
                )
                logger(
                    "deselect_tracks",
                    "$currentTrack $checkedId",
                )
                ijkVideoView?.selectTrack(checkedId)
            }
        }
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

    fun initView() {
        //mHudView = findViewById(R.id.hud_view)
        ijkVideoView = findViewById(R.id.ijk_media_view)
        recyclerViewPlayList = findViewById(R.id.recycler_playlist_ijk)
        drawerLayout = findViewById(R.id.drawer_layout_ijk)
        navigationView = findViewById(R.id.nav_view_vlc)
        mediaController = VideoControlView(this)
        mediaController.setAnchorView(ijkVideoView as ViewGroup)
        initButtons()
        addSource()
        ijkVideoView?.setMediaController(mediaController)
//        ijkVideoView?.setHudView(null)
    }

    private fun initButtons() {
        playerTitle = mediaController.findViewById(R.id.textView_title_vlc)
        playerDesc = mediaController.findViewById(R.id.textView_desc_vlc)
        playerLang = mediaController.findViewById(R.id.textView_language_vlc)
        backButton = mediaController.findViewById(R.id.vlc_back)
        menuButton = mediaController.findViewById(R.id.vlc_menu)

        forwardButton = mediaController.findViewById(R.id.vlc_forward)
        rewindButton = mediaController.findViewById(R.id.vlc_rew)
        pauseButton = mediaController.findViewById(R.id.vlc_pause)

        audioTrackSelector = mediaController.findViewById(R.id.vlc_track_selector)
        castButton = mediaController.findViewById(R.id.vlc_custom_cast)

        mNextButton = mediaController.findViewById(R.id.playlistNext)
        mPrevButton = mediaController.findViewById(R.id.playlistPrev)

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
            ijkVideoView?.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        } else {
            val controller = ijkVideoView?.windowInsetsController
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

    //drawer functions
    private fun showDownloadDialog() {
        downloaderUtils.showDownloadDialog(this::hideSystemUi, object : IResultListener {
            override fun workResult(result: Any) {
                btnClick = result as String
                goAction()
            }
        })
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
            ijkVideoView?.pause()
            val intentNow = PlayerUtils.createViewIntent(playerModelNow)
            if (intentNow.resolveActivity(packageManager) != null) {
                startActivity(intentNow)
            } else {
                GlobalFunctions.toaster(this, GlobalFunctions.NO_APP_FOUND_PLAY_MESSAGE)
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
                ijkVideoView?.pause()
                showDownloadDialog()
            }
            R.id.menu_open_with_other_app -> {
                ijkVideoView?.showMediaInfo()
                //adActivity(4)
            }
            R.id.menu_resize -> {
                logger("resize", "here")
                item.title = MeasureHelper.getAspectRatioText(
                    this,
                    ijkVideoView?.toggleAspectRatio() ?: IRenderView.AR_ASPECT_FILL_PARENT
                )
            }
            R.id.menu_playlist -> {
                if (recyclerViewPlayList.visibility == View.VISIBLE) {
                    recyclerViewPlayList.visibility = View.GONE
                } else {
                    recyclerViewPlayList.visibility = View.VISIBLE
                }
            }
            R.id.menu_change_player -> {
                GlobalFunctions.areYouSureDialogue(
                    this,
                    "Player wll change to ExoPlayer, Are you sure?",
                    this::yesSure
                )
            }

        }
        return false
    }

    private fun yesSure() {
        val intent = GlobalFunctions.getIntentPlayer(this, PlayerModel.STREAM_ONLINE_LIVE)
        intent.putExtra(PlayerModel.SELECTED_MODEL, idxNow)
        startActivity(intent)
        finish()
    }
}