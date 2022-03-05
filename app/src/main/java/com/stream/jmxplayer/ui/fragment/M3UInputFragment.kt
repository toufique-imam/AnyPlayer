package com.stream.jmxplayer.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.adapter.GalleryItemViewHolder
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.viewmodel.DatabaseViewModel
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.toaster
import com.stream.jmxplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.stream.jmxplayer.utils.m3u.OnScrappingCompleted
import com.stream.jmxplayer.utils.m3u.Parser
import com.stream.jmxplayer.utils.m3u.Scrapper
import java.net.URLDecoder

class M3UInputFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter

    private val viewModel: DatabaseViewModel by viewModels()
    private var playerModelNow = PlayerModel(-1)
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheetLinearLayout: LinearLayout
    private lateinit var titleTextViewM3U: TextView
    private lateinit var playButtonBottomSheet: MaterialButton
    private lateinit var editButtonBottomSheet: MaterialButton
    private lateinit var shareButtonBottomSheet: MaterialButton
    private lateinit var deleteButtonBottomSheet: MaterialButton
    private lateinit var directionButton: ImageView
    private lateinit var fabAddM3U: FloatingActionButton
    private lateinit var scrapper: Scrapper
    lateinit var alertDialogLoading: AlertDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_input_m3u, container, false)
        recyclerView = view.findViewById(R.id.recycler_m3u)
        fabAddM3U = view.findViewById(R.id.fab_add_m3u)
        scrapper = Scrapper(requireContext(), "")
        initBottomSheet(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        alertDialogLoading = requireActivity().createAlertDialogueLoading()

        galleryAdapter = GalleryAdapter(GalleryItemViewHolder.M3U_LIST,
            { video, _ -> historyItemClicked(video, true) },
            { video, _ -> historyItemClicked(video, false) }
        )
        recyclerView.also { viewR ->
            viewR.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            viewR.adapter = galleryAdapter
        }

        viewModel.videos.observe(viewLifecycleOwner) { videos ->
            galleryAdapter.updateData(videos)
        }

        if (requireActivity().intent.getBooleanExtra(PlayerUtils.M3U_INTENT, false)) {
            playerModelNow = PlayListAll[0]
            viewModel.insertModel(playerModelNow)
            galleryAdapter.addData(playerModelNow)
            historyItemClicked(playerModelNow, false)
        }
        viewModel.getAllM3U()

        fabAddM3U.setOnClickListener {
            createM3UDialog(false)
        }
    }

    var alertDialog: AlertDialog? = null
    private lateinit var titleInput: TextInputEditText
    private lateinit var linkInput: TextInputEditText
    private lateinit var userAgentInput: TextInputEditText
    private lateinit var acceptButton: MaterialButton
    private lateinit var editButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private fun initM3UDialog() {
        val dialogView = requireActivity().layoutInflater.inflate(
            R.layout.custom_dialog_user_input, null
        )
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        builder.setTitle("Stream Personal")
        alertDialog = builder.create()
        titleInput = dialogView.findViewById(R.id.text_view_custom_stream_name)
        linkInput = dialogView.findViewById(R.id.text_view_custom_stream_url)
        userAgentInput = dialogView.findViewById(R.id.text_view_custom_stream_user_agent)
        acceptButton = dialogView.findViewById(R.id.button_stream_confirm)
        editButton = dialogView.findViewById(R.id.button_stream_edit)
        cancelButton = dialogView.findViewById(R.id.button_stream_cancel)

        acceptButton.setOnClickListener {
            if (linkChecker()) {
                alertDialog?.dismiss()
                userInputM3U(true)
            } else {
                linkInput.error = "Link error"
                linkInput.requestFocus()
            }
        }
        editButton.setOnClickListener {
            if (linkChecker()) {
                alertDialog?.dismiss()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                userInputM3U(false)
                toaster(requireActivity(), "updated")
            } else {
                linkInput.error = "Link error"
                linkInput.requestFocus()
            }
        }
        cancelButton.setOnClickListener {
            alertDialog?.dismiss()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            toaster(requireActivity(), "Cancelled")
        }

    }

    private fun createM3UDialog(editing: Boolean) {
        if (alertDialog == null) initM3UDialog()
        if (editing) {
            acceptButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            linkInput.setText(playerModelNow.link)
            titleInput.setText(playerModelNow.title)
            if (playerModelNow.userAgent != GlobalFunctions.USER_AGENT)
                userAgentInput.setText(playerModelNow.userAgent)
        } else {
            acceptButton.visibility = View.VISIBLE
            editButton.visibility = View.GONE
            cancelButton.visibility = View.GONE
            linkInput.text?.clear()
            titleInput.text?.clear()
            userAgentInput.text?.clear()
        }

        alertDialog?.show()
    }

    private fun initBottomSheet(view: View) {
        bottomSheetLinearLayout = view.findViewById(R.id.linear_layout_bottom_sheet)
        titleTextViewM3U = view.findViewById(R.id.text_view_title_bs)
        playButtonBottomSheet = view.findViewById(R.id.material_button_play_bs)
        editButtonBottomSheet = view.findViewById(R.id.material_button_edit_bs)
        shareButtonBottomSheet = view.findViewById(R.id.material_button_share_bs)
        deleteButtonBottomSheet = view.findViewById(R.id.material_button_delete_bs)

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLinearLayout)
        directionButton = view.findViewById(R.id.image_view_direction_bs)
        directionButton.setOnClickListener {
            if (playerModelNow.id != -1L) {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    directionButton.rotation = 0F
                } else {
                    directionButton.rotation = 180F
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
        })
        bottomSheetBehavior.isDraggable = false
        playButtonBottomSheet.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            parseM3U(playerModelNow.link, playerModelNow.userAgent)
        }
        editButtonBottomSheet.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            createM3UDialog(true)
        }
        shareButtonBottomSheet.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            val intentNow = PlayerUtils.createViewIntent(playerModelNow)
            if (intentNow.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intentNow)
            } else {
                toaster(requireActivity(), GlobalFunctions.NO_APP_FOUND_PLAY_MESSAGE)
            }
        }
        deleteButtonBottomSheet.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            deleteModel(playerModelNow)
            scrapper.updateUrl(playerModelNow.link)
            scrapper.deletePrevious()
            playerModelNow = PlayerModel(-1)

            historyItemClicked(playerModelNow, false)
        }

    }

    fun m3uDataActionNew() {
        //toaster(requireActivity(), "m3uDataActionNew")
        findNavController()
            .navigate(R.id.action_streamFragment_to_m3uDisplayCategoryFragment)
    }

    private fun deleteModel(playerModel: PlayerModel) {
        galleryAdapter.deleteData(playerModel)
        viewModel.deleteModel(playerModel)
    }

    private fun historyItemClicked(playerModel: PlayerModel, isClicked: Boolean) {
        playerModelNow = playerModel
        bottomSheetBehavior.isDraggable = playerModelNow.id != -1L
        if (playerModel.id != -1L) {
            val title = "${playerModel.title}\n${playerModel.link}"
            titleTextViewM3U.text = title
            if (isClicked)
                parseM3U(playerModelNow.link, playerModelNow.userAgent)
            else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } else {
            titleTextViewM3U.setText(R.string.bottom_sheet_header_none)
        }
    }

    private fun linkChecker(): Boolean {
        if (!linkInput.checkUrl()) {
            linkInput.error = resources.getString(R.string.enter_stream_link)
            linkInput.requestFocus()
            return false
        }
        return true
    }

    private fun userInputM3U(newInsert: Boolean) {
        val urlNow = linkInput.text.toString()
        toaster(requireActivity(), urlNow)
        val uri = Uri.parse(urlNow)
        val token = URLDecoder.decode(
            if (titleInput.text != null && titleInput.text.toString()
                    .isNotEmpty()
            ) {
                titleInput.text.toString()
            } else {
                uri.lastPathSegment ?: "User M3U"
            }, "UTF-8"
        )
        val userAgentNow = userAgentInput.text.toString().ifEmpty { GlobalFunctions.USER_AGENT }
        SharedPreferenceUtils.setUserM3U(requireContext(), urlNow, token)
        val playerModel =
            PlayerModel(
                link = urlNow,
                streamType = PlayerModel.STREAM_M3U,
                title = token,
                userAgent = userAgentNow
            )
        playerModel.id =
            PlayerModel.getId(playerModel.link, playerModel.title, playerModel.streamType)
        if (!newInsert) {
            deleteModel(playerModelNow)
        }
        viewModel.insertModel(playerModel)
        historyItemClicked(playerModel, false)
        recyclerView.smoothScrollToPosition(galleryAdapter.addData(playerModel))
    }

    private fun parseM3U(urlNow: String, userAgent: String) {
        scrapper.updateUrl(urlNow)
        val loading = requireActivity().createAlertDialogueLoading()
        scrapper.onFinish(object : OnScrappingCompleted {
            override fun onComplete(response: String) {
                //logger("m3uParse", response)
                val data = Parser.ParseM3UString(response, userAgent)
                val dataNew = Parser.ParseM3UStringWithCategory(response, userAgent)
                M3uDisplayCategoryFragment.categoryData = dataNew
                PlayListAll.clear()
                PlayListAll.addAll(data)
                loading.dismiss()
                if (data.isNotEmpty()) {
                    m3uDataActionNew()
                } else {
                    toaster(requireActivity(), "Empty List/Parsing Failed")
                }
            }

            override fun onError() {
                loading.dismiss()
                toaster(requireActivity(), "Error Occurred")
            }
        })
        loading.show()
        scrapper.startScrapping()
    }

    companion object {
        @JvmStatic
        fun newInstance() = M3UInputFragment()
    }

}