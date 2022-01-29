package com.stream.jmxplayer.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.mediarouter.app.MediaRouteButton
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.github.javiersantos.piracychecker.PiracyChecker
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stream.jmxplayer.R
import com.stream.jmxplayer.casty.Casty
import com.stream.jmxplayer.model.IAdListener
import com.stream.jmxplayer.model.ICastController
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.fragment.BrowseFragment
import com.stream.jmxplayer.ui.fragment.WebViewFragment
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.isProVersion
import com.stream.jmxplayer.utils.GlobalFunctions.toaster
import com.stream.jmxplayer.utils.ijkplayer.Settings


class BrowserActivity : AppCompatActivity(), ICastController {
    var adMobAdUtils: AdMobAdUtils? = null
    lateinit var alertDialog: AlertDialog
    lateinit var iAdListener: IAdListener
    lateinit var bottomNavBar: BottomNavigationView
    lateinit var navHostFragment: NavHostFragment
    var piracyChecker: PiracyChecker? = null
    var inWebView = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(Settings(this).themeId)
        //setTheme(SharedPreferenceUtils.getTheme(this))
        setContentView(R.layout.activity_browser)
        piracyChecker = initPiracy {}
        alertDialog = createAlertDialogueLoading()
        adMobAdUtils = AdMobAdUtils(this)
        initAdListener()
        adMobAdUtils?.setAdListener(iAdListener)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        initCast()
        bottomNavBar = findViewById(R.id.bottom_navigation_view)
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { controller, _, _ ->
            toolbar.title = controller.currentDestination?.label ?: toolbar.title
            if (controller.currentDestination?.id == R.id.historyFragment) {
                adMobAdUtils?.loadFullScreenAd()
            }
            if (controller.currentDestination?.id == R.id.m3uDisplayCategoryFragment) {
                adMobAdUtils?.loadFullScreenAd()
            }
            inWebView = controller.currentDestination?.id == R.id.webviewFragment
        }
        NavigationUI.setupWithNavController(bottomNavBar, navController)
        if (intent.getBooleanExtra(PlayerUtils.M3U_INTENT, false)) {
            navController.navigate(R.id.streamFragment)
        }
    }

    private fun initAdListener() {
        iAdListener = object : IAdListener {
            override fun onAdActivityDone(result: String) {
                alertDialog.dismiss()
            }

            override fun onAdLoadingStarted() {
                alertDialog.show()
            }

            override fun onAdLoaded(type: Int) {
                alertDialog.dismiss()

                if (type == 0) adMobAdUtils?.showFullScreenAd()
                else adMobAdUtils?.showRewardAd()
            }

            override fun onAdError(error: String) {
                alertDialog.dismiss()
                GlobalFunctions.logger("Splash Ad", error)
            }
        }
    }

    var casty: Casty? = null

    private fun initCast() {
        casty = Casty.create(this)

        casty?.setOnConnectChangeListener(object : Casty.OnConnectChangeListener {
            override fun onConnected() {
                toaster(this@BrowserActivity, "connected")
                if (navHostFragment.navController.currentDestination?.id == R.id.browseFragment) {
                    (navHostFragment.childFragmentManager.fragments[0] as BrowseFragment).showImageCast()
                }
            }

            override fun onDisconnected() {
                toaster(this@BrowserActivity, "disconnected")
            }
        })
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.clear()
        //menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (!isProVersion() && item.itemId == R.id.action_pro) {
            showProMode {
                adMobAdUtils?.loadRewardAd()
            }
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        piracyChecker?.destroy()
    }

    override fun castPlayerModel(playerModel: PlayerModel) {
        if (casty?.isConnected == true) {
            casty?.player?.loadMediaAndPlay(
                PlayerUtils.createMediaData(
                    playerModel
                )
            )
        }
    }

    override fun onBackPressed() {
        if (inWebView) {
            var webFragment: WebViewFragment? = null
            for (x in navHostFragment.childFragmentManager.fragments) {
                if (x is WebViewFragment) {
                    webFragment = x
                    break
                }
            }
            if (webFragment == null) super.onBackPressed()
            if (webFragment?.goBack() == false) super.onBackPressed()
        } else
            super.onBackPressed()
    }

    override fun updateCastButton(mediaRouteButton: MediaRouteButton) {
        casty?.setUpMediaRouteButton(mediaRouteButton)
    }

}