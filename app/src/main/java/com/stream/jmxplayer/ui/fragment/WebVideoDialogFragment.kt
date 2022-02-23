package com.stream.jmxplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stream.jmxplayer.adapter.WebVideoAdapter
import com.stream.jmxplayer.databinding.WebOverlayVideoItemsBinding
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.viewmodel.WebVideoViewModel
import com.stream.jmxplayer.utils.setVisible

class WebVideoDialogFragment(
    onClick: (PlayerModel, Int) -> Unit
) : JMXBottomSDFragment() {
    private lateinit var binding: WebOverlayVideoItemsBinding
    lateinit var onBindInitiated: (done: Boolean) -> Unit

    private val webVideoViewModel: WebVideoViewModel by lazy {
        ViewModelProvider(requireActivity()).get(WebVideoViewModel::class.java)
    }

    override fun getDefaultState(): Int = BottomSheetBehavior.STATE_EXPANDED


    override fun needToManageOrientation(): Boolean = true

    override fun initialFocusedView(): View = binding.emptyView

    private val webAdapter = WebVideoAdapter(onClick)


    fun onWebVideoChanged() {
        binding.webList.adapter = webAdapter
        webVideoViewModel.videos.observe(viewLifecycleOwner) { videos ->
            webAdapter.updateData(videos.toList())
            if (videos.isNullOrEmpty()) {
                binding.emptyView.setVisible()
            }
        }
        webVideoViewModel.getDownloadData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = WebOverlayVideoItemsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.webList.layoutManager = LinearLayoutManager(requireContext())

        super.onViewCreated(view, savedInstanceState)
        onBindInitiated(true)

    }
}