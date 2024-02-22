package com.retroline.anyplayer.ui.fragment

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.retroline.anyplayer.R
import com.retroline.anyplayer.adapter.GalleryAdapter
import com.retroline.anyplayer.adapter.GalleryItemViewHolder
import com.retroline.anyplayer.model.ICastController
import com.retroline.anyplayer.model.IStoragePermission
import com.retroline.anyplayer.model.PlayerModel
import com.retroline.anyplayer.ui.view.ImageOverlayView
import com.retroline.anyplayer.ui.viewmodel.DatabaseViewModel
import com.retroline.anyplayer.ui.viewmodel.LocalVideoViewModel
import com.retroline.anyplayer.utils.GlobalFunctions
import com.retroline.anyplayer.utils.GlobalFunctions.getGridSpanCount
import com.retroline.anyplayer.utils.GlobalFunctions.toaster
import com.retroline.anyplayer.utils.NotificationCenter
import com.retroline.anyplayer.utils.Settings
import com.retroline.anyplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.stfalcon.imageviewer.StfalconImageViewer
import io.reactivex.rxjava3.disposables.CompositeDisposable

class BrowseFragment : Fragment() {
    private val disposables = CompositeDisposable()

    private val viewModel: LocalVideoViewModel by viewModels()
    private val historyViewModel: DatabaseViewModel by viewModels()
    private lateinit var gallery: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var openAlbum: MaterialButton
    private lateinit var grantPermissionButton: MaterialButton
    private lateinit var welcomeView: LinearLayout
    private lateinit var permissionRationaleView: LinearLayout
    lateinit var galleryAdapter: GalleryAdapter
    var typeNow = PlayerModel.STREAM_OFFLINE_VIDEO
    var lastSelectedTab: Int = 0
    private var overlayView: ImageOverlayView? = null
    private var imageViewer: StfalconImageViewer<PlayerModel>? = null
    private var iCastController: ICastController? = null
    val mSettings: Settings by lazy {
        Settings(requireContext())
    }

    private var playerModelNow = PlayerModel(-1)

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

    private fun initTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    lastSelectedTab = tab.position
                    val prevType = typeNow
                    when (tab.text) {
                        resources.getString(R.string.video) -> typeNow =
                            PlayerModel.STREAM_OFFLINE_VIDEO

                        resources.getString(R.string.audio) -> typeNow =
                            PlayerModel.STREAM_OFFLINE_AUDIO

                        resources.getString(R.string.image) -> typeNow =
                            PlayerModel.STREAM_OFFLINE_IMAGE
                    }
                    if (prevType != typeNow) {
                        openMediaStore()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    private fun initCastListener() {
        try {
            iCastController = context as ICastController
        } catch (exception: ClassCastException) {
            toaster(requireActivity(), "no implemented " + exception.message)
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        initCastListener()
    }


    fun showImageCast() {
        iCastController?.castPlayerModel(playerModelNow)
    }

    private fun setupOverlay(position: Int) {
        overlayView = ImageOverlayView(requireContext()).apply {
            playerModelNow = galleryAdapter.galleryData[position]
            update(playerModelNow)
            historyViewModel.insertModel(playerModelNow)
            onBackClick = {
                imageViewer?.close()
                imageViewer = null
            }
        }
        if (iCastController == null) initCastListener()
        iCastController?.updateCastButton(overlayView!!.castButton)
    }

    private fun showImages(position: Int) {
        setupOverlay(position)
        imageViewer = StfalconImageViewer.Builder(
            context,
            galleryAdapter.galleryData
        ) { view, imageNow ->
            Glide.with(view).load(imageNow.image).into(view)
        }
            .withImageChangeListener {
                playerModelNow = galleryAdapter.galleryData[it]
                historyViewModel.insertModel(playerModelNow)
                overlayView?.update(playerModelNow)
                showImageCast()
            }
            .withStartPosition(position)
            .withHiddenStatusBar(true)
            .withOverlayView(overlayView).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        disposables.add(NotificationCenter.shared.observe().subscribe { event ->
            if (event == NotificationCenter.PermissionCallback) {
                permissionCallback()
            }
        })
        tabLayout = view.findViewById(R.id.tab_custom_browse)
        gallery = view.findViewById(R.id.gallery)
        openAlbum = view.findViewById(R.id.open_album)
        grantPermissionButton = view.findViewById(R.id.grant_permission_button)
        welcomeView = view.findViewById(R.id.welcome_view)
        permissionRationaleView = view.findViewById(R.id.permission_rationale_view)

        galleryAdapter = GalleryAdapter(GalleryItemViewHolder.GRID_NO_DELETE, { video, pos ->
            if (video.streamType != PlayerModel.STREAM_OFFLINE_IMAGE) {
                val intent = GlobalFunctions.getDefaultPlayer(requireContext(), mSettings, video)
                PlayListAll.clear()
                PlayListAll.add(video)
                startActivity(intent)
            } else {
                showImages(pos)
            }
        }, { _, _ -> })
        galleryAdapter.setHasStableIds(true)
        val spanCount = getGridSpanCount(requireActivity())

        gallery.also { viewR ->
            viewR.layoutManager = GridLayoutManager(context, spanCount)
            viewR.adapter = galleryAdapter
        }
        viewModel.videos.observe(viewLifecycleOwner) { videos ->
            galleryAdapter.updateData(videos)
        }
        openAlbum.setOnClickListener {
            openMediaStore()
        }
        grantPermissionButton.setOnClickListener { openMediaStore() }
        if (!haveStoragePermission()) {
            welcomeView.visibility = View.VISIBLE
        } else {
            showVideos()
        }
        initTabLayout()
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
        searchView.queryHint = SEARCH_VIEW_QUERY_HINT
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_refresh) {
            openMediaStore()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun showVideos() {
        if (!haveStoragePermission()) {
            requestPermission()
            return
        }
        viewModel.loadMedia(typeNow)
        //galleryAdapter.notifyDataSetChanged()
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
    private fun haveStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_MEDIA_IMAGES
            ) == PermissionChecker.PERMISSION_GRANTED)
        ) {
            // Full access on Android 13 (API level 33) or higher
            true
        } else // Access denied or Full access up to Android 12 (API level 32)
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PermissionChecker.PERMISSION_GRANTED
    }

    /**
     * Convenience method to request [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
     */
    private fun getPermissions() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private fun requestPermission() {
        if (!haveStoragePermission()) {
            (activity as? IStoragePermission)?.getRequestPermissions()?.launch(getPermissions())
        }
    }

    private fun permissionCallback() {
        if (haveStoragePermission()) {
            openMediaStore()
        } else {
            val showRationale = requireActivity().shouldShowRequestPermissionRationale(
                getPermissions()[0]
            )
            if (showRationale) {
                showNoAccess()
            } else {
                goToSettings()
            }
        }
    }

    private fun goToSettings() {
        Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${activity?.packageName}")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }

    companion object {
        const val TAB_LAYOUT_VIDEO = "Video"
        const val TAB_LAYOUT_AUDIO = "Audio"
        const val TAB_LAYOUT_IMAGE = "Image"
        const val SEARCH_VIEW_QUERY_HINT = "Search channel"
    }
}