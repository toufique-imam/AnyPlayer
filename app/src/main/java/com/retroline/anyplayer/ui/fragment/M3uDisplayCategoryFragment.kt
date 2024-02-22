package com.retroline.anyplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.retroline.anyplayer.R
import com.retroline.anyplayer.model.PlayerModel


class M3uDisplayCategoryFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var collectionAdapter: ViewPagerAdapterCategory

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_m3u_display_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val categories = ArrayList<String>(categoryData.keys)

        tabLayout = view.findViewById(R.id.tab_m3u_category)
        viewPager2 = view.findViewById(R.id.viewpager_m3u_category)
        tabLayout.tabMode =
            if (categories.size < 4) TabLayout.MODE_FIXED else TabLayout.MODE_SCROLLABLE
        collectionAdapter = ViewPagerAdapterCategory(this, categories)
        viewPager2.adapter = collectionAdapter
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = categories[position]
        }.attach()
    }

    class ViewPagerAdapterCategory(fragment: Fragment, private val categories: ArrayList<String>) :
        FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = categories.size

        override fun createFragment(position: Int): Fragment {
            val categoryName = categories[position]
            return CategoryFragment.newInstance(categoryName)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment M3uDisplayCategoryFragment.
         */
        @JvmStatic
        fun newInstance() = M3uDisplayCategoryFragment()

        var categoryData = HashMap<String, ArrayList<PlayerModel>>()

    }
}