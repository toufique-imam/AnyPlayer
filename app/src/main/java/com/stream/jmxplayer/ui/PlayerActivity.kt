package com.stream.jmxplayer.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.mediarouter.app.MediaRouteButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.javiersantos.piracychecker.PiracyChecker
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.util.Util
import com.google.android.material.navigation.NavigationView
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.adapter.GalleryItemViewHolder
import com.stream.jmxplayer.casty.Casty
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.fragment.players.BasePlayerFragment
import com.stream.jmxplayer.ui.fragment.players.ExoPlayerFragment
import com.stream.jmxplayer.ui.view.VideoControlView
import com.stream.jmxplayer.ui.viewmodel.DatabaseViewModel
import com.stream.jmxplayer.utils.*

class PlayerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    Player.Listener, AnalyticsListener {

    private lateinit var casty: Casty
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

    private lateinit var nextButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var audioTrackSelector: ImageButton
    private var playerDialog: AlertDialog? = null
    private lateinit var castButton: MediaRouteButton
    private lateinit var recyclerViewPlayList: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var resizeUtils: ResizeUtils
    private lateinit var mediaController: VideoControlView


    private lateinit var fragmentContainerView: FrameLayout

    //    private lateinit var historyDB: HistoryDatabase
    private val viewModel: DatabaseViewModel by viewModels()
    lateinit var mSettings: Settings
    var idxNow = 0
    private var fromError = false
    var inErrorState = false
    var errorCount = 0
    private var piracyChecker: PiracyChecker? = null
    private lateinit var playerModelNow: PlayerModel
    private var exoFragment: ExoPlayerFragment? = null
    private var currentFragment: BasePlayerFragment? = null
    var autoPlay = true
//    private var ijkFragment: IJKPlayerFragment? = null
//    private var vlcFragment: VLCPlayerFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSettings = Settings(this)
        setTheme(mSettings.themeId)
        setContentView(R.layout.activity_player)
        getFragments(savedInstanceState)
        piracyChecker = initPiracy {}
        getDataFromIntent()
        allFindViewByID()
        setUpOrientation()
        resizeUtils = ResizeUtils(this)
        animationUtils = MAnimationUtils(this)
        exoFragment?.let { switchToFragment(it) }
        setUpDrawer()
        setUpAppBar()
        updateTexts()
        initCast()
        setUpMenuButton()
        initPlaylist()
        setUpPlayerViewControl()
    }

    private fun getFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            exoFragment = supportFragmentManager.getFragment(
                savedInstanceState,
                EXO_FRAGMENT
            ) as ExoPlayerFragment
//            ijkFragment = supportFragmentManager.getFragment(savedInstanceState, IJK_FRAGMENT) as IJKPlayerFragment
//            vlcFragment = supportFragmentManager.getFragment(savedInstanceState, VLC_FRAGMENT) as VLCPlayerFragment
        } else {
            exoFragment = ExoPlayerFragment.newInstance()
        }
    }


    private fun switchToFragment(fragment: BasePlayerFragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (currentFragment != null) fragmentTransaction.remove(currentFragment!!)
        fragmentTransaction.replace(R.id.fragment_media_view, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        currentFragment = fragment
        mediaController.setMediaPlayer(currentFragment)
    }

    private fun initPlayer(fromError: Boolean) {
        currentFragment?.setPlayerModel(playerModelNow)
        //currentFragment?.initPlayer(fromError)
        if (!fromError) {
            addToHistory(playerModelNow)

        }
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initPlayer(false)
        }
    }

    override fun onResume() {
        super.onResume()
        currentFragment?.hideSystemUI()
        if (Util.SDK_INT <= 23) {
            initPlayer(false)
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            currentFragment?.releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            currentFragment?.releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        piracyChecker?.destroy()
    }
    private fun initButtons() {
        playerTitle = mediaController.findViewById(R.id.textView_title_vlc)
        playerDesc = mediaController.findViewById(R.id.textView_desc_vlc)
        playerLang = mediaController.findViewById(R.id.textView_language_vlc)

        backButton = mediaController.findViewById(R.id.vlc_back)
        menuButton = mediaController.findViewById(R.id.vlc_menu)

        forwardButton = mediaController.findViewById(R.id.vlc_forward)
        rewindButton = mediaController.findViewById(R.id.vlc_rew)
//        pauseButton = mediaController.findViewById(R.id.vlc_pause)

        audioTrackSelector = mediaController.findViewById(R.id.vlc_track_selector)
        castButton = mediaController.findViewById(R.id.vlc_custom_cast)

        nextButton = mediaController.findViewById(R.id.playlistNext)
        previousButton = mediaController.findViewById(R.id.playlistPrev)

    }
    private fun allFindViewByID() {
        mediaController = VideoControlView(this)

//        playerStyleCommon = findViewById(R.id.player_style_common)
        fragmentContainerView = findViewById(R.id.fragment_media_view)
        recyclerViewPlayList = findViewById(R.id.recycler_playlist)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        mediaController.setAnchorView(fragmentContainerView)
        mediaController.show()
        initButtons()
        recyclerViewAutoHide()
    }

    fun setUpPlayerStyle(){
        navigationView.setOnClickListener{

        }
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
            R.id.menu_resize -> {
                changeResize()
                item.title =
                    resources.getString(R.string.resize) + " : " + resizeUtils.resizeMode.valueStr
                drawerLayout.closeDrawer(GravityCompat.END)
            }
            R.id.menu_playlist -> {
                if (recyclerViewPlayList.visibility == View.VISIBLE) {
                    recyclerViewPlayList.visibility = View.GONE
                } else {
                    recyclerViewPlayList.visibility = View.VISIBLE
                }
                drawerLayout.closeDrawer(GravityCompat.END)
            }
            R.id.menu_change_player -> {
                fromError = false
                if (playerDialog == null)
                    playerDialog =
                        createPlayerDialogue(Settings.PV_PLAYER__IjkExoMediaPlayer, this::yesSure)
                drawerLayout.closeDrawer(GravityCompat.END)
                playerDialog?.show()
            }
            R.id.menu_media_info -> {
                drawerLayout.closeDrawer(GravityCompat.END)
                currentFragment?.showMediaInfo()
            }
        }
        return false
    }

    private fun yesSure(playerID: Int) {
        if (playerID == Settings.PV_PLAYER__IjkExoMediaPlayer) return
        currentFragment?.releasePlayer()
//        val intent = GlobalFunctions.getIntentPlayer(this, playerID)
//        intent.putExtra(PlayerModel.SELECTED_MODEL, idxNow)
//        intent.putExtra(IJKPlayerActivity.FROM_ERROR, fromError)
//        finish()
//        startActivity(intent)
        //todo change fragment

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

    private fun setUpAppBar() {
        backButton.setOnClickListener {
            currentFragment = null
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
            currentFragment?.hideSystemUI()
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
        currentFragment?.changeResize(resizeUtils.setResize())
    }

    private fun updatePlayerModel() {
        playerModelNow = SharedPreferenceUtils.PlayListAll[idxNow]
        updateTexts()
        addToHistory(playerModelNow)
        if (casty.isConnected) {
            casty.player.loadMediaAndPlay(PlayerUtils.createMediaData(playerModelNow))
        }
        currentFragment?.setPlayerModel(playerModelNow)
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
//        animationUtils.setMidFocusExoControl(
//            playButton,
//            R.drawable.ic_play_red,
//            R.drawable.ic_play
//        )
//        animationUtils.setMidFocusExoControl(
//            pauseButton,
//            R.drawable.ic_pause_red,
//            R.drawable.ic_pause
//        )
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

        nextButton.setOnClickListener {
            idxNow++
            if (SharedPreferenceUtils.PlayListAll.isNotEmpty())
                idxNow %= SharedPreferenceUtils.PlayListAll.size
            updatePlayerModel()
        }
        previousButton.setOnClickListener {
            idxNow--
            if (SharedPreferenceUtils.PlayListAll.isNotEmpty()) {
                idxNow += SharedPreferenceUtils.PlayListAll.size
                idxNow %= SharedPreferenceUtils.PlayListAll.size
            }
            updatePlayerModel()
        }
        audioTrackSelector.setOnClickListener {
            currentFragment?.mediaOptionButtonClicked()
        }
//        playButton.setOnClickListener {
//            GlobalFunctions.logger(ExoPlayerActivity.TAG, "play clicked")
//            currentFragment?.playButtonClicked()
//            toggleButton()
//        }
//
//        pauseButton.setOnClickListener {
//            GlobalFunctions.logger(ExoPlayerActivity.TAG, "pause clicked")
//            currentFragment?.pauseButtonClicked()
//            toggleButton()
//        }

    }

    private fun updateTexts() {
        setUpAppBarTexts(
            playerModelNow.title,
            playerModelNow.description,
            playerModelNow.mLanguage
        )
    }


    private fun recyclerViewAutoHide() {
        fragmentContainerView.setOnClickListener {
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
        galleryAdapter.updateData(SharedPreferenceUtils.PlayListAll)
        recyclerViewPlayList.visibility = View.GONE
    }


    private fun initCast() {
        casty = Casty.create(this).withMiniController()
        //Casty.configure("8639B975")
        casty.setUpMediaRouteButton(castButton)
        casty.setOnConnectChangeListener(object : Casty.OnConnectChangeListener {
            override fun onConnected() {
                GlobalFunctions.toaster(this@PlayerActivity, "connected")
                casty.player.loadMediaAndPlay(PlayerUtils.createMediaData(playerModelNow))
            }

            override fun onDisconnected() {
                GlobalFunctions.toaster(this@PlayerActivity, "disconnected")
                //todo hideSystemUi()
            }
        })
    }

    private fun getDataFromIntent() {
        val intent: Intent
        if (getIntent() != null) {
            intent = getIntent()
            idxNow = intent.getIntExtra(PlayerModel.SELECTED_MODEL, 0)
        } else {
            idxNow = 0
        }
        if (SharedPreferenceUtils.PlayListAll.size > idxNow)
            playerModelNow = SharedPreferenceUtils.PlayListAll[idxNow]
        else if (SharedPreferenceUtils.PlayListAll.isNotEmpty()) {
            playerModelNow = SharedPreferenceUtils.PlayListAll[0]
            idxNow = 0
        }
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
                currentFragment?.hideSystemUI()
            }
        }
    }

    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        if (loadEventInfo.uri.toString() == ExoPlayerActivity.MAGNA_TV_BLOCKED) {
            playerModelNow.link = ""
            inErrorState = true
            currentFragment?.releasePlayer()
            GlobalFunctions.toaster(this, ExoPlayerActivity.SERVER_ERROR_TEXT)
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
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
                nextTrack()
            }
            else -> {
                stateString = "UNKNOWN"
            }
        }
        GlobalFunctions.logger(ExoPlayerActivity.TAG, "Player state changed: $stateString")
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        super<Player.Listener>.onPlayerError(error)
        GlobalFunctions.logger(ExoPlayerActivity.TAG, "Error : " + error.message)
        GlobalFunctions.logger(ExoPlayerActivity.TAG, "Error Cause : " + error.cause)
        inErrorState = true
        errorCount++
        GlobalFunctions.logger(ExoPlayerActivity.TAG, "Error count : $errorCount")
        if (errorCount == 1) {
            inErrorState = false
            autoPlay = true
            currentFragment?.clearResumePosition()
            //mPlayer?.next()
            initPlayer(true)
            return
        }

        if (ExoPlayerActivity.isDecoderError(error)) {
            GlobalFunctions.logger(ExoPlayerActivity.TAG, "Error Decoder")
            inErrorState = false
            //errorCount = 0
            currentFragment?.releasePlayer()
            initPlayer(true)
        }
        if (ExoPlayerActivity.isBehindLiveWindow(error)) {
            GlobalFunctions.logger(ExoPlayerActivity.TAG, "behind")
            currentFragment?.clearResumePosition()
            initPlayer(true)
        } else {
            GlobalFunctions.logger(ExoPlayerActivity.TAG, "Else $fromError")
            if (!fromError) {
                fromError = true
                yesSure(Settings.PV_PLAYER__IjkMediaPlayer)
            } else {
                fromError = false
                nextTrack()
            }
        }
    }

    private fun nextTrack() {
        if (SharedPreferenceUtils.PlayListAll.isNotEmpty() && SharedPreferenceUtils.PlayListAll.size > 1) {
            fromError = false
            if (SharedPreferenceUtils.PlayListAll.isNotEmpty()) {
                idxNow++
                idxNow %= SharedPreferenceUtils.PlayListAll.size
            }
            updatePlayerModel()
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (inErrorState) currentFragment?.updateResumePosition()
    }

    companion object {
        const val EXO_FRAGMENT = "EXO_FRAGMENT"
        const val IJK_FRAGMENT = "IJK_FRAGMENT"
        const val VLC_FRAGMENT = "VLC_FRAGMENT"
    }
}