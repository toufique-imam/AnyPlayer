package com.stream.jmxplayer.ui.fragment

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.adapter.GalleryItemViewHolder
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.viewmodel.DatabaseViewModel
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.stream.jmxplayer.utils.ijkplayer.Settings

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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gallery = view.findViewById(R.id.recycler_gallery)
        galleryAdapter = GalleryAdapter(
            GalleryItemViewHolder.SINGLE_DELETE,
            { video, _ ->
                //val intent = Intent(context, PlayerActivity::class.java)
                val intent =
                    GlobalFunctions.getIntentPlayer(
                        requireContext(),
                        Settings.PV_PLAYER__IjkMediaPlayer
                    )
                PlayListAll.clear()
                PlayListAll.add(video)
                startActivity(intent)
            }, { video, pos ->
                deleteHistory(video)
            }
        )
        galleryAdapter.setHasStableIds(true)

        gallery.also { viewR ->
            viewR.layoutManager = GridLayoutManager(context, 1)
            viewR.adapter = galleryAdapter
        }
        viewModel.videos.observe(viewLifecycleOwner, { videos ->
            galleryAdapter.updateData(videos)
        })
        viewModel.getAll()
        //galleryAdapter.updateData(historyDatabase.playerModelDao().getAll())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        if (GlobalFunctions.isProVersion()) {
            inflater.inflate(R.menu.toolbar_history_pro, menu)
        } else {
            inflater.inflate(R.menu.toolbar_history, menu)
        }
        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search_history)?.actionView as SearchView
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