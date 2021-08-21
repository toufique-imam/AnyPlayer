package com.stream.jmxplayer.ui

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.model.PlayerModel.Companion.DIRECT_PUT

/** The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission. */
private const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045

class BrowserActivity : AppCompatActivity() {
    private val viewModel: LocalVideoViewModel by viewModels()
    lateinit var gallery: RecyclerView
    lateinit var openAlbum: MaterialButton
    lateinit var grantPermissionButton: MaterialButton
    lateinit var welcomeView: LinearLayout
    lateinit var permissionRationaleView: LinearLayout
    lateinit var toolbar: MaterialToolbar
    lateinit var galleryAdapter: GalleryAdapter
    private fun initViews() {
        gallery = findViewById(R.id.gallery)
        openAlbum = findViewById(R.id.open_album)
        grantPermissionButton = findViewById(R.id.grant_permission_button)
        welcomeView = findViewById(R.id.welcome_view)
        permissionRationaleView = findViewById(R.id.permission_rationale_view)
        toolbar = findViewById(R.id.toolbar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        initViews()
        setSupportActionBar(toolbar)
        galleryAdapter = GalleryAdapter { video ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra(DIRECT_PUT, video)
            startActivity(intent)
        }
        gallery.also { view ->
            view.layoutManager = GridLayoutManager(this, 2)
            view.adapter = galleryAdapter
        }
        viewModel.videos.observe(this, { videos ->
            galleryAdapter.updateData(videos)
        })
        openAlbum.setOnClickListener {
            openMediaStore()
        }
        grantPermissionButton.setOnClickListener { openMediaStore() }
        if (!haveStoragePermission()) {
            welcomeView.visibility = View.VISIBLE
        } else {
            showVideos()
        }
    }

    private fun showVideos() {
        viewModel.loadVideos()
        welcomeView.visibility = View.GONE
        permissionRationaleView.visibility = View.GONE
    }

    private fun openMediaStore() {
        if (haveStoragePermission()) {
            showVideos()
        } else {
            requestPermission()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.clear()
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu?.findItem(R.id.action_search)?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.maxWidth = Int.MAX_VALUE
        searchView.queryHint = "Buscar canal"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                galleryAdapter.filter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                galleryAdapter.filter.filter(newText)
                return true
            }

        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.action_refresh -> {
                openMediaStore()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }


    private fun showNoAccess() {
        welcomeView.visibility = View.GONE
        permissionRationaleView.visibility = View.VISIBLE
    }

    private fun goToSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    showVideos()
                } else {
                    val showRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    if (showRationale) {
                        showNoAccess()
                    } else {
                        goToSettings()
                    }
                }
                return
            }
        }
    }

    /**
     * Convenience method to check if [Manifest.permission.READ_EXTERNAL_STORAGE] permission
     * has been granted to the app.
     */
    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PERMISSION_GRANTED

    /**
     * Convenience method to request [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
     */
    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, READ_EXTERNAL_STORAGE_REQUEST)
        }
    }
}