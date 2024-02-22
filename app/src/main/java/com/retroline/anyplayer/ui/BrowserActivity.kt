package com.retroline.anyplayer.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.mediarouter.app.MediaRouteButton
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.github.javiersantos.piracychecker.PiracyChecker
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.retroline.anyplayer.R
import com.retroline.anyplayer.casty.Casty
import com.retroline.anyplayer.model.IAdListener
import com.retroline.anyplayer.model.ICastController
import com.retroline.anyplayer.model.IStoragePermission
import com.retroline.anyplayer.model.PlayerModel
import com.retroline.anyplayer.ui.fragment.BrowseFragment
import com.retroline.anyplayer.utils.AdMobUtils
import com.retroline.anyplayer.utils.GlobalFunctions
import com.retroline.anyplayer.utils.GlobalFunctions.toaster
import com.retroline.anyplayer.utils.NotificationCenter
import com.retroline.anyplayer.utils.PlayerUtils
import com.retroline.anyplayer.utils.Settings
import com.retroline.anyplayer.utils.createAlertDialogueLoading
import com.retroline.anyplayer.utils.initPiracy


class BrowserActivity : AppCompatActivity(), ICastController, IAdListener, IStoragePermission {
    private var adMobUtils: AdMobUtils? = null
    private lateinit var alertDialog: AlertDialog
    private lateinit var bottomNavBar: BottomNavigationView
    lateinit var navHostFragment: NavHostFragment
    private var piracyChecker: PiracyChecker? = null
    private var requestPermissions: ActivityResultLauncher<Array<String>>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(Settings(this).themeId)
        requestPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                // Handle permission requests results
                // See the permission example in the Android platform samples: https://github.com/android/platform-samples
                for (i in results.keys) {
                    GlobalFunctions.logger("result", i + " " + results[i])
                }
                NotificationCenter.shared.post(NotificationCenter.PermissionCallback)
            }

        //setTheme(SharedPreferenceUtils.getTheme(this))
        setContentView(R.layout.activity_browser)
        piracyChecker = initPiracy {}
        alertDialog = createAlertDialogueLoading()
        adMobUtils = AdMobUtils(this)
        //initAdListener()
        adMobUtils?.setAdListener(this)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        initCast()
        bottomNavBar = findViewById(R.id.bottom_navigation_view)
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { controller, _, _ ->
            toolbar.title = controller.currentDestination?.label ?: toolbar.title
            when (controller.currentDestination?.id) {
                R.id.historyFragment -> {
                    loadAd(0) {}
                }
                R.id.m3uDisplayCategoryFragment -> {
                    loadAd(0) {}
                }
            }
        }
        NavigationUI.setupWithNavController(bottomNavBar, navController)
        if (intent.getBooleanExtra(PlayerUtils.M3U_INTENT, false)) {
            navController.navigate(R.id.streamFragment)
        }
    }

    private var afterAd: (() -> Unit)? = null
    private fun loadAd(type: Int, afterAd: () -> Unit) {
        this.afterAd = afterAd
        if (type == 0) adMobUtils?.loadFullScreenAd()
        else adMobUtils?.loadRewardAd()
    }
    override fun getRequestPermissions(): ActivityResultLauncher<Array<String>>? =
        requestPermissions


    override fun onAdActivityDone(result: String) {
        alertDialog.dismiss()
        afterAd?.invoke()
    }

    override fun onAdLoadingStarted() {
        alertDialog.show()
    }

    override fun onAdLoaded(type: Int) {
        alertDialog.dismiss()

        if (type == 0) adMobUtils?.showFullScreenAd()
        else adMobUtils?.showRewardAd()
    }

    override fun onAdError(error: String) {
        alertDialog.dismiss()
        GlobalFunctions.logger("Splash Ad", error)
        afterAd?.invoke()
    }


    private var casty: Casty? = null

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
        return if (item.itemId == R.id.action_pro) {
                loadAd(1) {}
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

    override fun updateCastButton(mediaRouteButton: MediaRouteButton) {
        casty?.setUpMediaRouteButton(mediaRouteButton)
    }

}