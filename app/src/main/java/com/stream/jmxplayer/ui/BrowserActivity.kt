package com.stream.jmxplayer.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.github.javiersantos.piracychecker.PiracyChecker
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.IAdListener
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.ijkplayer.Settings


class BrowserActivity : AppCompatActivity() {
    var adMobAdUtils: AdMobAdUtils? = null
    lateinit var alertDialog: AlertDialog
    lateinit var iAdListener: IAdListener
    var piracyChecker: PiracyChecker? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(Settings(this).themeId)
        //setTheme(SharedPreferenceUtils.getTheme(this))
        setContentView(R.layout.activity_browser)
        piracyChecker = initPiracy()
        alertDialog = createAlertDialogueLoading()
        adMobAdUtils = AdMobAdUtils(this)
        initAdListener()
        adMobAdUtils?.setAdListener(iAdListener)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val bottomNavBar = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { controller, _, _ ->
            toolbar.title = controller.currentDestination?.label ?: toolbar.title
            if (controller.currentDestination?.id == R.id.browseFragment) {
                adMobAdUtils?.loadFullScreenAd()
            }
            if (controller.currentDestination?.id == R.id.historyFragment) {
                adMobAdUtils?.loadFullScreenAd()
            }
            if (controller.currentDestination?.id == R.id.m3uDisplayCategoryFragment) {
                adMobAdUtils?.loadFullScreenAd()
            }
        }
        NavigationUI.setupWithNavController(bottomNavBar, navController)
        //todo check code
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


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.clear()
        //menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (!GlobalFunctions.isProVersion() && item.itemId == R.id.action_pro) {
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

}