package com.stream.jmxplayer.ui.fragment

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.adapter.GalleryItemViewHolder
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.SharedPreferenceUtils

class M3UDisplayFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    lateinit var galleryAdapter: GalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_gallery)
        galleryAdapter = GalleryAdapter(GalleryItemViewHolder.GRID_NO_DELETE, { _, pos ->
            //val intent = Intent(context, PlayerActivity::class.java)
//            val intent = Intent(context, VlcActivity::class.java)
            val intent = GlobalFunctions.getIntentPlayer(requireContext(), PlayerModel.STREAM_M3U)
            intent.putExtra(PlayerModel.SELECTED_MODEL, pos)
            startActivity(intent)
        }, { _, _ -> })
        galleryAdapter.setHasStableIds(true)
        galleryAdapter.updateData(SharedPreferenceUtils.PlayListAll)
        val spanCount = GlobalFunctions.getGridSpanCount(requireActivity())

        recyclerView.also { viewR ->
            viewR.layoutManager = GridLayoutManager(context, spanCount)
            viewR.adapter = galleryAdapter
        }
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

    companion object {
        @JvmStatic
        fun newInstance() =
            M3UDisplayFragment()
    }
}