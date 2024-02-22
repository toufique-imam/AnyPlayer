package com.retroline.anyplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.retroline.anyplayer.R

class StreamFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var collectionAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.non_pro, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stream, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //init views
        tabLayout = view.findViewById(R.id.tab_custom_stream)
        viewPager2 = view.findViewById(R.id.viewpager_stream)
        tabLayout.tabMode = TabLayout.MODE_FIXED

        collectionAdapter = ViewPagerAdapter(this)
        viewPager2.adapter = collectionAdapter
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = if (position == 0) "M3U List" else "Stream Link"
        }.attach()
    }


    class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        private val userLinkFragment0 = UserLinkFragment.newInstance()
        private val userLinkFragment1 = M3UInputFragment.newInstance()
        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                userLinkFragment1
            } else {
                userLinkFragment0
            }
        }
    }
}