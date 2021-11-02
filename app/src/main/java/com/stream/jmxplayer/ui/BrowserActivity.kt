package com.stream.jmxplayer.ui

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.IAdListener
import com.stream.jmxplayer.utils.AdMobAdUtils
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.toaster
import com.stream.jmxplayer.utils.createAlertDialogueLoading
import com.stream.jmxplayer.utils.ijkplayer.Settings


class BrowserActivity : AppCompatActivity() {
    var adMobAdUtils: AdMobAdUtils? = null
    lateinit var alertDialog: AlertDialog
    lateinit var iAdListener: IAdListener
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(Settings(this).themeId)
        //setTheme(SharedPreferenceUtils.getTheme(this))
        setContentView(R.layout.activity_browser)
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
                adMobAdUtils!!.loadAd()
            }
            if (controller.currentDestination?.id == R.id.historyFragment) {
                adMobAdUtils!!.loadAd()
            }
        }
        NavigationUI.setupWithNavController(bottomNavBar, navController)
    }

    private fun initAdListener() {
        iAdListener = object : IAdListener {
            override fun onAdActivityDone(result: String) {
                alertDialog.dismiss()
            }

            override fun onAdLoadingStarted() {
                alertDialog.show()
            }

            override fun onAdLoaded() {
                alertDialog.dismiss()
                adMobAdUtils?.showAd()
            }

            override fun onAdError(error: String) {
                alertDialog.dismiss()
                toaster(this@BrowserActivity, "Ad error $error")
                GlobalFunctions.logger("Splash Ad", error)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.clear()
        //menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

}