package com.stream.jmxplayer.ui.fragment

import android.app.SearchManager
import android.content.Context
import android.content.Intent
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
import com.stream.jmxplayer.model.db.HistoryDatabase
import com.stream.jmxplayer.model.db.SharedPreferenceUtils.Companion.PlayListAll
import com.stream.jmxplayer.ui.PlayerActivity


/**
 * A simple [Fragment] subclass.
 * Use the [HistoryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HistoryFragment : Fragment() {

    private lateinit var gallery: RecyclerView
    lateinit var galleryAdapter: GalleryAdapter
    private lateinit var historyDatabase: HistoryDatabase
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
        historyDatabase = HistoryDatabase.getInstance(requireContext())
        gallery = view.findViewById(R.id.recycler_gallery)
        galleryAdapter = GalleryAdapter(
            GalleryItemViewHolder.SINGLE_DELETE,
            { video, _ ->
                val intent = Intent(context, PlayerActivity::class.java)
                PlayListAll.clear()
                PlayListAll.add(video)
                startActivity(intent)
            }, { video, pos ->
                deleteHistory(video, pos)
            }
        )
        galleryAdapter.setHasStableIds(true)

        gallery.also { viewR ->
            viewR.layoutManager = GridLayoutManager(context, 1)
            viewR.adapter = galleryAdapter
        }
        galleryAdapter.updateData(historyDatabase.playerModelDao().getAll())
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
        historyDatabase.playerModelDao().deleteAll()
        galleryAdapter.updateData(historyDatabase.playerModelDao().getAll())
    }

    private fun deleteHistory(playerModel: PlayerModel, pos: Int) {
        historyDatabase.playerModelDao().deleteModel(playerModel)
        galleryAdapter.deleteData(pos)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.action_delete_history -> {
                deleteHistory()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment HistoryFragment.
         */
        @JvmStatic
        fun newInstance() = HistoryFragment()
    }
}