package com.stream.jmxplayer.ui


import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.WindowInsetsController.*
import android.view.WindowManager.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.mediarouter.app.MediaRouteButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.util.Util
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.adapter.GalleryItemViewHolder
import com.stream.jmxplayer.casty.Casty
import com.stream.jmxplayer.model.*
import com.stream.jmxplayer.model.PlayerModel.Companion.SELECTED_MODEL
import com.stream.jmxplayer.mpv.MPVPlayer
import com.stream.jmxplayer.ui.IJKPlayerActivity.Companion.FROM_ERROR
import com.stream.jmxplayer.ui.view.IjkVideoView
import com.stream.jmxplayer.ui.view.TableLayoutBinder
import com.stream.jmxplayer.ui.viewmodel.DatabaseViewModel
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.toaster
import com.stream.jmxplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.stream.jmxplayer.utils.ijkplayer.Settings
import kotlin.math.max

@Suppress("Deprecation")
class ExoPlayerActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    Player.Listener,
    AnalyticsListener {

    private val _autoPlay = "autoplay"
    private val _currentWindowIndex = "current_window_index"
    private val _playbackPosition = "playback_position"

    private lateinit var alertDialogLoading: AlertDialog
    private lateinit var playerModelNow: PlayerModel

    //private var playList = ArrayList<PlayerModel>()
    private lateinit var btnClick: String

    private var autoPlay = true
    private var inErrorState = false
    private var errorCount = 0

    private var mPlayer: BasePlayer? = null

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
    private lateinit var nextButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var audioTrackSelector: ImageButton

    private var trackSelectorDialog = ArrayList<Dialog?>()
    private var trackDialog: AlertDialog? = null
    private var playerDialog: AlertDialog? = null
    private lateinit var castButton: MediaRouteButton

    private val loadControl = DefaultLoadControl()
    private lateinit var trackSelector: DefaultTrackSelector

    private lateinit var recyclerViewPlayList: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter


    lateinit var adMobAdUtils: AdMobAdUtils
    private lateinit var iAdListener: IAdListener

    private lateinit var downloaderUtils: DownloaderUtils
    private lateinit var resizeUtils: ResizeUtils

    //    private lateinit var historyDB: HistoryDatabase
    private val viewModel: DatabaseViewModel by viewModels()
    lateinit var mSettings: Settings

    var mediaMetaData: MediaMetadata? = null

    var playerNow = Settings.PV_PLAYER__IjkExoMediaPlayer

    var idxNow = 0
    /*
    Life Cycle
     */

    private fun showMediaInfo() {
        mediaMetaData = mPlayer?.mediaMetadata
        if (mediaMetaData == null) return

        val builder = TableLayoutBinder(this)
        builder.appendSection(R.string.mi_player)
        builder.appendRow2(R.string.mi_player, "ExoPlayer")
        builder.appendSection(R.string.mi_media)
        if (mPlayer is SimpleExoPlayer) {
            builder.appendRow2(
                R.string.mi_resolution,
                IjkVideoView.buildResolution(
                    (mPlayer as SimpleExoPlayer).videoFormat?.width ?: 0,
                    (mPlayer as SimpleExoPlayer).videoFormat?.height ?: 0,
                    0, 0
                )
            )
        }
        builder.appendRow2(
            R.string.mi_length,
            IjkVideoView.buildTimeMilli(mPlayer?.duration ?: 0)
        )
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        var index = -1
        if (mappedTrackInfo != null) {
            for (i in 0 until mappedTrackInfo.rendererCount) {
                val trackGroupArray = mappedTrackInfo.getTrackGroups(i)
                val trackType = mappedTrackInfo.getRendererType(i)
                val typeStr = when (trackType) {
                    C.TRACK_TYPE_VIDEO -> getString(R.string.TrackType_video)
                    C.TRACK_TYPE_AUDIO -> getString(R.string.TrackType_audio)
                    C.TRACK_TYPE_TEXT -> getString(R.string.TrackType_subtitle)
                    C.TRACK_TYPE_METADATA -> getString(R.string.TrackType_metadata)
                    C.TRACK_TYPE_DEFAULT -> "Default"
                    else -> getString(R.string.TrackType_unknown)
                }
                for (j in 0 until trackGroupArray.length) {
                    val y: TrackGroup = trackGroupArray[j]
                    index++
                    if (trackSelected(mPlayer?.currentTrackGroups, y)) {
                        builder.appendSection(
                            getString(R.string.mi_stream_fmt1, index) + " " +
                                    getString(R.string.mi__selected_video_track)
                        )
                    } else {
                        builder.appendSection(
                            getString(
                                R.string.mi_stream_fmt1,
                                index
                            )
                        )
                    }
                    if (y.length > 0) {
                        val format = y.getFormat(0)
                        builder.appendRow2(R.string.mi_type, typeStr)
                        builder.appendRow2(R.string.mi_language, format.language)

                        if (trackType == C.TRACK_TYPE_AUDIO || trackType == C.TRACK_TYPE_VIDEO) {
                            builder.appendRow2(
                                R.string.mi_codec, format.codecs
                            )
                            if (trackType == C.TRACK_TYPE_VIDEO) {
                                builder.appendRow2(
                                    R.string.mi_pixel_format,
                                    format.pixelCount.toString()
                                )
                                builder.appendRow2(
                                    R.string.mi_resolution,
                                    IjkVideoView.buildResolution(format.height, format.width, 0, 0)
                                )
                                builder.appendRow2(
                                    R.string.mi_frame_rate,
                                    format.frameRate.toString()
                                )
                            } else {
                                builder.appendRow2(
                                    R.string.mi_sample_rate,
                                    format.sampleRate.toString()
                                )
                                builder.appendRow2(
                                    R.string.mi_channels,
                                    format.channelCount.toString()
                                )
                            }

                            builder.appendRow2(
                                R.string.mi_bit_rate,
                                format.averageBitrate.toString()
                            )
                        }
                    }
                }
            }
        }
        val adBuilder = builder.buildAlertDialogBuilder()
        adBuilder.setTitle(R.string.media_information)
        adBuilder.setNegativeButton(R.string.close, null)
        adBuilder.show()
    }

    private fun trackSelected(
        trackSelected: TrackGroupArray?,
        trackGroupNow: TrackGroup
    ): Boolean {
        if (trackSelected != null) {
            for (i in 0 until trackSelected.length) {
                if (trackSelected[i] == trackGroupNow) return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logger("onCreate", "called")
        super.onCreate(savedInstanceState)
        mSettings = Settings(this)
        setTheme(mSettings.themeId)
        setContentView(R.layout.activity_player)
        alertDialogLoading = GlobalFunctions.createAlertDialogueLoading(this)

        //historyDB = HistoryDatabase.getInstance(this)
        getDataFromIntent()
        logger("Intent", "$playerNow")
        setUpOrientation()

        downloaderUtils = DownloaderUtils(this, playerModelNow)
        resizeUtils = ResizeUtils(this)
        animationUtils = MAnimationUtils(this)
        allFindViewByID()
        setUpDrawer()
        setUpAppBar()
        updateTexts()
        initCast()
        setUpMenuButton()
        initPlaylist()
        setUpPlayerViewControl()

        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong(_playbackPosition, 0)
            currentWindow = savedInstanceState.getInt(_currentWindowIndex)
            autoPlay = savedInstanceState.getBoolean(_autoPlay)
        }

        adMobAdUtils = AdMobAdUtils(this)
    }

    private fun initCast() {
        casty = Casty.create(this).withMiniController()
        //Casty.configure("8639B975")
        casty.setUpMediaRouteButton(castButton)
        casty.setOnConnectChangeListener(object : Casty.OnConnectChangeListener {
            override fun onConnected() {
                toaster(this@ExoPlayerActivity, "connected")
                casty.player.loadMediaAndPlayInBackground(PlayerUtils.createMediaData(playerModelNow))
            }

            override fun onDisconnected() {
                toaster(this@ExoPlayerActivity, "disconnected")
                hideSystemUi()
            }
        })
    }

    private fun initPlayerSelect() {
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_tracks, null)
        val builder = AlertDialog.Builder(this).setTitle("Select Player").setView(dialogView)
        builder.setCancelable(true)
        val radioGroup: RadioGroup = dialogView.findViewById(R.id.radio_group_server)
        playerDialog = builder.create()

        val playerList = resources.getStringArray(R.array.pref_entries_player_select)
        val playerId = arrayOf(
            Settings.PV_PLAYER__IjkMediaPlayer,
            Settings.PV_PLAYER__IjkExoMediaPlayer,
            Settings.PV_PLAYER__AndroidMediaPlayer
        )
        for (i in 1 until playerList.size) {
            val radioButton = RadioButton(this)
            radioButton.id = playerId[i - 1]
            radioButton.text = playerList[i]
            radioButton.textSize = 15f
            radioButton.setTextColor(Color.BLACK)
            if (playerId[i - 1] == playerNow) {
                radioButton.isChecked = true
            }
            radioGroup.addView(radioButton)
        }
        val materialButton: MaterialButton = dialogView.findViewById(R.id.button_stream_now)


        materialButton.setOnClickListener {
            playerDialog?.dismiss()
            //playerNow = radioGroup.checkedRadioButtonId
            yesSure(radioGroup.checkedRadioButtonId)
        }
    }

    private fun initTrackSelector() {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        if (mappedTrackInfo == null) return else audioTrackSelector.visibility = View.VISIBLE
        trackSelectorDialog.clear()
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_tracks, null)

        val builder = AlertDialog.Builder(this).setTitle("Select Tracks").setView(dialogView)
        builder.setCancelable(true)
        val radioGroup: RadioGroup = dialogView.findViewById(R.id.radio_group_server)

        trackDialog = builder.create()
        var cnt = 0
        trackSelectorDialog.clear()
        for (i in 0 until mappedTrackInfo.rendererCount) {
            if (isVideoRenderer(mappedTrackInfo, i) || isAudioRenderer(
                    mappedTrackInfo,
                    i
                ) || isSubtitleRenderer(mappedTrackInfo, i)
            ) {
                trackSelectorDialog.add(
                    TrackSelectionDialogBuilder(
                        this,
                        "Select audio track",
                        trackSelector,
                        i
                    ).build()
                )
                val radioButton = RadioButton(this)
                radioButton.id = cnt
                radioButton.text = mappedTrackInfo.getRendererName(i)
                radioButton.textSize = 15f
                radioButton.setTextColor(Color.BLACK)
                if (cnt == 0) {
                    radioButton.isChecked = true
                }
                radioGroup.addView(radioButton)
                cnt++
            }
        }
        if (cnt == 0)
            audioTrackSelector.visibility = View.GONE

        val materialButton: MaterialButton = dialogView.findViewById(R.id.button_stream_now)


        materialButton.setOnClickListener {
            trackDialog?.dismiss()
            trackSelectorDialog[radioGroup.checkedRadioButtonId]?.show()
        }
    }

    override fun onStart() {
        logger("Came here", "onStart")
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer(false)
        }
    }

    override fun onResume() {
        super.onResume()
        logger("Came here", "onResume")
        // start in pure full screen
        hideSystemUi()
        if (Util.SDK_INT <= 23) {
            initPlayer(false)
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
        //stopCastServer()
    }

    private fun setUpOrientation() {
        window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT < 30) {
            window.setFlags(
                LayoutParams.FLAG_FULLSCREEN,
                LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

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

    private fun allFindViewByID() {
        recyclerViewPlayList = findViewById(R.id.recycler_playlist)
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

        nextButton = findViewById(R.id.playlistNext)
        previousButton = findViewById(R.id.playlistPrev)
        audioTrackSelector = mPlayerView.findViewById(R.id.exo_track_selector)

        recyclerViewAutoHide()

    }

    private fun recyclerViewAutoHide() {
        mPlayerView.setOnClickListener {
            if (recyclerViewPlayList.visibility == View.VISIBLE) {
                recyclerViewPlayList.visibility = View.GONE
            }
        }
    }

    private fun initPlaylist() {
        galleryAdapter = GalleryAdapter(
            GalleryItemViewHolder.SINGLE_NO_DELETE,
            { _, pos ->
                idxNow = pos
                updatePlayerModel()
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
            mPlayer?.pause()
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
    private fun updateTexts() {
        setUpAppBarTexts(
            playerModelNow.title,
            playerModelNow.description,
            playerModelNow.mLanguage
        )
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
        animationUtils.setMidFocusExoControl(
            playButton,
            R.drawable.ic_baseline_play_circle_red_filled_24,
            R.drawable.ic_play
        )
        animationUtils.setMidFocusExoControl(
            pauseButton,
            R.drawable.ic_baseline_pause_circle_red_filled_24,
            R.drawable.ic_play
        )
        nextButton.setOnClickListener {
            idxNow++
            if (PlayListAll.isNotEmpty())
                idxNow %= PlayListAll.size
            updatePlayerModel()
        }
        previousButton.setOnClickListener {
            idxNow--
            if (PlayListAll.isNotEmpty()) {
                idxNow += PlayListAll.size
                idxNow %= PlayListAll.size
            }
            updatePlayerModel()
        }
        audioTrackSelector.setOnClickListener {
            if (trackDialog == null)
                initTrackSelector()
            trackDialog?.show()
        }
    }

    private fun isAudioRenderer(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        renderedIndex: Int
    ): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(renderedIndex)
        if (trackGroupArray.length == 0) return false

        val trackType = mappedTrackInfo.getRendererType(renderedIndex)
        return C.TRACK_TYPE_AUDIO == trackType
    }

    private fun isSubtitleRenderer(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        renderedIndex: Int
    ): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(renderedIndex)
        if (trackGroupArray.length == 0) return false

        val trackType = mappedTrackInfo.getRendererType(renderedIndex)
        return C.TRACK_TYPE_TEXT == trackType
    }

    private fun isVideoRenderer(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        renderedIndex: Int
    ): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(renderedIndex)
        if (trackGroupArray.length == 0) return false

        val trackType = mappedTrackInfo.getRendererType(renderedIndex)
        return C.TRACK_TYPE_VIDEO == trackType
    }


    private fun updatePlayerModel() {
        playerModelNow = PlayListAll[idxNow]
        updateTexts()
        if (casty.isConnected) {
            casty.player.loadMediaAndPlay(PlayerUtils.createMediaData(playerModelNow))
        }
        addSource()
        preparePlayer()
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

    private fun setUpAppBarTexts(title: String, desc: String, lang: String) {
        setTitleText(title, playerTitle)
        playerDesc.text = desc
        playerLang.text = lang
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
        // mPlayerView.layoutParams = resizeUtils.setAspectRatio(requestedOrientation)
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
            R.id.menu_playlist -> {
                if (recyclerViewPlayList.visibility == View.VISIBLE) {
                    recyclerViewPlayList.visibility = View.GONE
                } else {
                    recyclerViewPlayList.visibility = View.VISIBLE
                }
            }
            R.id.menu_change_player -> {
                fromError = false
                if (playerDialog == null) initPlayerSelect()
                playerDialog?.show()
            }
            R.id.menu_media_info -> {
                showMediaInfo()
            }
        }
        return false
    }

    private fun yesSure(playerID: Int) {
        logger("YesSure", "$playerID")
        if (playerID == playerNow) return
        playerNow = playerID
        if (playerID == Settings.PV_PLAYER__IjkExoMediaPlayer || playerID == Settings.PV_PLAYER__AndroidMediaPlayer) {
            releasePlayer()
            errorCount = 0
            initPlayer(false)
            return
        }
        releasePlayer()
        val intent = GlobalFunctions.getPlayer(this, playerID)
        intent.putExtra(SELECTED_MODEL, idxNow)
        intent.putExtra(FROM_ERROR, fromError)
        startActivity(intent)
        finish()
    }

    /*
    PlayerModel Input / Parsing
     */

    private fun addToHistory(playerModel: PlayerModel) {
//        val playerDao = historyDB.playerModelDao()
//        playerDao.insertModel(playerModel)
        viewModel.insertModel(playerModel)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(PlayerModel.DIRECT_PUT, playerModelNow)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateTexts()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        playerModelNow = savedInstanceState.getSerializable(PlayerModel.DIRECT_PUT) as PlayerModel
        updateTexts()
    }

    private fun getDataFromIntent() {
        val intent: Intent
        if (getIntent() != null) {
            intent = getIntent()
            idxNow = intent.getIntExtra(SELECTED_MODEL, 0)
            fromError = intent.getBooleanExtra(FROM_ERROR, false)
            playerNow =
                intent.getIntExtra(GlobalFunctions.PLAYER_ID, Settings.PV_PLAYER__IjkExoMediaPlayer)
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

    /*
    ExoPlayer Functions
     */
    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT < 30) {
            window.setFlags(
                LayoutParams.FLAG_FULLSCREEN,
                LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(
                    WindowInsets.Type.statusBars()
                            or WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior =
                    BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        if (Build.VERSION.SDK_INT < 30) {
            mPlayerView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        } else {
            val controller = mPlayerView.windowInsetsController
            if (controller != null) {
                controller.hide(
                    (WindowInsets.Type.statusBars()
                            or WindowInsets.Type.navigationBars()
                            or WindowInsets.Type.systemBars())
                )
                controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun addSource() {
        trackSelectorDialog.clear()
        trackDialog = null
        audioTrackSelector.visibility = View.GONE
        addToHistory(playerModelNow)
        val mediaSource = PlayerUtils.createMediaSource(this, playerModelNow, errorCount)
        if (mPlayer is SimpleExoPlayer)
            (mPlayer as SimpleExoPlayer).setMediaSource(mediaSource)
        else mPlayer?.setMediaItem(mediaSource.mediaItem)
    }

    private fun createPlayer() {
        trackSelector = DefaultTrackSelector(this)
        trackSelector.setParameters(
            trackSelector.parameters.buildUpon()
                .setMaxVideoSizeSd()
                .setPreferredTextLanguage("es")
                .setPreferredAudioLanguage("es")
        )

        logger(TAG, "render " + mSettings.renderExo)
        @DefaultRenderersFactory.ExtensionRendererMode val extensionRendererMode =
            when (mSettings.renderExo) {
                1 -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                2 -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                3 -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                else -> {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
                        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    else
                        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                }
            }

        val renderer = DefaultRenderersFactory(this)
            .setExtensionRendererMode(extensionRendererMode)
        mPlayer = if (playerNow == Settings.PV_PLAYER__IjkExoMediaPlayer) {
            logger(TAG, "simpleExoPlayer")
            SimpleExoPlayer.Builder(this, renderer)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .setSeekBackIncrementMs(15000)
                .setSeekForwardIncrementMs(15000)
                .build()
        } else {
            logger(TAG, "MPVPlayer")
            val preferredLanguages = mapOf(
                C.TRACK_TYPE_AUDIO to "spa",
                C.TRACK_TYPE_TEXT to "spa"
            )
            MPVPlayer(application, true, preferredLanguages, true)
        }

    }

    private fun preparePlayer() {
        mPlayer?.prepare()
        if (autoPlay) {
            mPlayer?.play()
        } else {
            mPlayer?.pause()
        }
    }

    private fun initPlayer(fromError: Boolean) {
        inErrorState = false
        if (mPlayer == null) {
            createPlayer()
            mPlayer?.playWhenReady = autoPlay
            mPlayer?.addListener(this)
            if (mPlayer is SimpleExoPlayer) {
                (mPlayer as SimpleExoPlayer).addAnalyticsListener(this)
            }
            mPlayerView.player = mPlayer
        }
        if (!fromError) {
            //addToHistory(playerModelNow)
            addSource()
        }
        preparePlayer()
    }

    private fun releasePlayer() {
        if (mPlayer != null) {
            trackDialog = null
            trackSelectorDialog.clear()
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
        playbackPosition = max(0, mPlayer?.contentPosition ?: 0)
    }

    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        logger("Loading", loadEventInfo.uri.toString())
        if (loadEventInfo.uri.toString() == MAGNA_TV_BLOCKED) {
            playerModelNow.link = ""
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
                errorCount = 0
                inErrorState = false
                audioTrackSelector.visibility = View.VISIBLE
            }
            ExoPlayer.STATE_ENDED -> {
                stateString = "STATE_ENDED"
                mPlayer?.next()
            }
            else -> {
                stateString = "UNKNOWN"
            }
        }
        logger(TAG, "changed state to $stateString")
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        super<Player.Listener>.onPlayerError(error)
        logger(TAG, "Error : " + error.message)
        logger(TAG, "Error Cause : " + error.cause)
        inErrorState = true
        errorCount++
        logger(TAG, "Error count : $errorCount")
        if (errorCount == 1) {
            inErrorState = false
            autoPlay = true
            clearResumePosition()
            //mPlayer?.next()
            initPlayer(true)
            return
        }

        if (isDecoderError(error)) {
            logger(TAG, "Error Decoder")
            inErrorState = false
            //errorCount = 0
            releasePlayer()
            initPlayer(true)
        }
        if (isBehindLiveWindow(error)) {
            logger(TAG, "behind")
            clearResumePosition()
            initPlayer(true)
        } else {
            logger(TAG, "Else $fromError")
            if (!fromError) {
                fromError = true
                yesSure(1)
            } else {
                fromError = false
                nextTrack()
            }
        }
    }

    private fun nextTrack() {
        if (PlayListAll.isNotEmpty() && PlayListAll.size > 1) {
            logger(TAG, "next track")
            fromError = false
            if (PlayListAll.isNotEmpty()) {
                idxNow++
                idxNow %= PlayListAll.size
            }
            updatePlayerModel()
        }
    }

    var fromError = false

    override fun onPositionDiscontinuity(reason: Int) {
        if (inErrorState) updateResumePosition()
    }

    companion object {
        const val SERVER_ERROR_TEXT =
            "Lo sentimos, canal no disponible utilice otro servidor para volver a intentar o vuelva más tarde"
        const val TAG = "ExoPlayer"
        const val MAGNA_TV_BLOCKED = "http://51.158.167.219/con/999_cna2.ts"
        fun isDecoderError(exception: PlaybackException): Boolean {
            return when (exception.errorCode) {
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> true
                PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> true
                PlaybackException.ERROR_CODE_DECODING_FAILED -> true
                PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> true
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> true
                else -> false
            }
        }

        fun isBehindLiveWindow(exception: PlaybackException): Boolean {
            if (exception.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                return true
            }
            return false
        }
    }
}