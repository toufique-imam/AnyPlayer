package com.stream.jmxplayer.ui

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stream.jmxplayer.R
import com.stream.jmxplayer.utils.ijkplayer.Settings


class BrowserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(Settings(this).themeId)
        //setTheme(SharedPreferenceUtils.getTheme(this))
        setContentView(R.layout.activity_browser)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val bottomNavBar = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { controller, _, _ ->
            toolbar.title = controller.currentDestination?.label ?: toolbar.title
        }
        NavigationUI.setupWithNavController(bottomNavBar, navController)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.clear()
        //menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

}