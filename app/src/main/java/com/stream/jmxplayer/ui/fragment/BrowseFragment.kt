package com.stream.jmxplayer.ui.fragment

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.LocalVideoViewModel
import com.stream.jmxplayer.ui.PlayerActivity
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.getGridSpanCount


/**
 * A simple [Fragment] subclass.
 * Use the [BrowseFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BrowseFragment : Fragment() {

    private val viewModel: LocalVideoViewModel by viewModels()
    private lateinit var gallery: RecyclerView
    private lateinit var openAlbum: MaterialButton
    private lateinit var grantPermissionButton: MaterialButton
    private lateinit var welcomeView: LinearLayout
    private lateinit var permissionRationaleView: LinearLayout
    lateinit var galleryAdapter: GalleryAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_browse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //initViews()
        gallery = view.findViewById(R.id.gallery)
        openAlbum = view.findViewById(R.id.open_album)
        grantPermissionButton = view.findViewById(R.id.grant_permission_button)
        welcomeView = view.findViewById(R.id.welcome_view)
        permissionRationaleView = view.findViewById(R.id.permission_rationale_view)
        galleryAdapter = GalleryAdapter { video ->
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra(PlayerModel.DIRECT_PUT, video)
            startActivity(intent)
        }
        galleryAdapter.setHasStableIds(true)
        val spanCount = getGridSpanCount(requireActivity())

        gallery.also { viewR ->
            viewR.layoutManager = GridLayoutManager(context, spanCount)
            viewR.adapter = galleryAdapter
        }
        viewModel.videos.observe(viewLifecycleOwner, { videos ->
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
        //menuItemActions()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.toolbar_browse, menu)
        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search)?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
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

    private fun showNoAccess() {
        welcomeView.visibility = View.GONE
        permissionRationaleView.visibility = View.VISIBLE
    }

    /**
     * Convenience method to check if [Manifest.permission.READ_EXTERNAL_STORAGE] permission
     * has been granted to the app.
     */
    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Convenience method to request [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
     */
    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissions,
                READ_EXTERNAL_STORAGE_REQUEST
            )
        }
    }

    private fun goToSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${activity?.packageName}")
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
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showVideos()
                } else {
                    val showRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
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


    companion object {
        /** The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission. */
        private const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment BrowseFragment.
         */
        @JvmStatic
        fun newInstance() = BrowseFragment()
    }
}