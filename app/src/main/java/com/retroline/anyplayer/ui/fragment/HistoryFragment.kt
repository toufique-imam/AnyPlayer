package com.retroline.anyplayer.ui.fragment

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.retroline.anyplayer.R
import com.retroline.anyplayer.adapter.GalleryAdapter
import com.retroline.anyplayer.adapter.GalleryItemViewHolder
import com.retroline.anyplayer.model.PlayerModel
import com.retroline.anyplayer.ui.view.ImageOverlayView
import com.retroline.anyplayer.ui.viewmodel.DatabaseViewModel
import com.retroline.anyplayer.utils.GlobalFunctions
import com.retroline.anyplayer.utils.Settings
import com.retroline.anyplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.retroline.anyplayer.utils.setGone
import com.stfalcon.imageviewer.StfalconImageViewer

class HistoryFragment : Fragment() {

    private lateinit var gallery: RecyclerView
    lateinit var galleryAdapter: GalleryAdapter
    private val viewModel: DatabaseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list_view, container, false)
    }

    var overlayView: ImageOverlayView? = null
    var imageViewer: StfalconImageViewer<PlayerModel>? = null
    var images: List<PlayerModel> = ArrayList()

    private fun showImage(position: Int) {
        if (images.size <= position) return
        overlayView = ImageOverlayView(requireContext()).apply {
            update(images[position])
            onBackClick = {
                imageViewer?.close()
                imageViewer = null
            }
            castButton.setGone()
        }
        imageViewer = StfalconImageViewer.Builder(
            context,
            images
        ) { view, imageNow ->
            Glide.with(view).load(imageNow.image).into(view)
        }
            .withImageChangeListener {
                if (images.size > it)
                    overlayView?.update(images[it])
            }
            .withStartPosition(position)
            .withHiddenStatusBar(true)
            .withOverlayView(overlayView).show()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gallery = view.findViewById(R.id.recycler_gallery)
        galleryAdapter = GalleryAdapter(
            GalleryItemViewHolder.SINGLE_DELETE,
            { video, _ ->
                //val intent = Intent(context, PlayerActivity::class.java)
                if (video.streamType == PlayerModel.STREAM_OFFLINE_IMAGE) {
                    images =
                        galleryAdapter.galleryData.filter { playerModel -> playerModel.streamType == PlayerModel.STREAM_OFFLINE_IMAGE }
                    val pos = images.indexOf(video)
                    showImage(pos)
                } else {
                    val intent =
                        GlobalFunctions.getIntentPlayer(
                            requireContext(),
                            Settings.PV_PLAYER__IjkMediaPlayer
                        )
                    PlayListAll.clear()
                    PlayListAll.add(video)
                    startActivity(intent)
                }
            }, { video, pos ->
                deleteHistory(video)
            }
        )
        galleryAdapter.setHasStableIds(true)

        gallery.also { viewR ->
            viewR.layoutManager = GridLayoutManager(context, 1)
            viewR.adapter = galleryAdapter
        }
        viewModel.videos.observe(viewLifecycleOwner) { videos ->
            galleryAdapter.updateData(videos)
        }
        viewModel.getAll()
        //galleryAdapter.updateData(historyDatabase.playerModelDao().getAll())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.toolbar_history, menu)
        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search_history)?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
        searchView.maxWidth = Int.MAX_VALUE
        searchView.queryHint = resources.getString(R.string.search)
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

    private fun deleteHistory() {
        viewModel.deleteAll()
    }

    private fun deleteHistory(playerModel: PlayerModel) {
        galleryAdapter.deleteData(playerModel)
        viewModel.deleteModel(playerModel)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_delete_history) {
            deleteHistory()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}