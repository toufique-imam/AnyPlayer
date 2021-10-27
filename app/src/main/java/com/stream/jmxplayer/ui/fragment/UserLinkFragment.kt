package com.stream.jmxplayer.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.adapter.GalleryItemViewHolder
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.viewmodel.DatabaseViewModel
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.toaster
import com.stream.jmxplayer.utils.PlayerUtils
import com.stream.jmxplayer.utils.SharedPreferenceUtils
import com.stream.jmxplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.stream.jmxplayer.utils.ijkplayer.Settings
import com.stream.jmxplayer.utils.m3u.OnScrappingCompleted
import com.stream.jmxplayer.utils.m3u.Parser
import com.stream.jmxplayer.utils.m3u.Scrapper
import java.net.URLDecoder

class UserLinkFragment : Fragment() {
    private lateinit var titleTextView: TextInputEditText
    private lateinit var linkTextView: TextInputEditText
    private lateinit var userAgentTextView: TextInputEditText
    private lateinit var headersTextView: TextInputEditText
    private lateinit var acceptButton: MaterialButton
    private lateinit var editConfirmButton: MaterialButton
    private lateinit var editCancelButton: MaterialButton
    private lateinit var bottomSheetLinearLayout: LinearLayout
    private lateinit var headerTIL: TextInputLayout
    private lateinit var userAgentTIL: TextInputLayout
    private lateinit var titleTIL: TextInputLayout
    private lateinit var optionalTV: TextView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var titleTextViewM3U: TextView
    private lateinit var playButton: MaterialButton
    private lateinit var editButton: MaterialButton
    private lateinit var shareButton: MaterialButton
    private lateinit var deleteButton: MaterialButton
    private lateinit var directionButton: ImageView

    private lateinit var recyclerView: RecyclerView

    private val viewModel: DatabaseViewModel by viewModels()

    //private lateinit var historyDatabase: HistoryDatabase
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var m3UDisplayFragment: M3UDisplayFragment

    private var fragmentType: Int = 0

    private var playerModelNow = PlayerModel(-1)
    lateinit var mSettings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fragmentType = it.getInt(ARG_TYPE)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_user_stream, container, false)
        titleTextView = view.findViewById(R.id.text_view_custom_stream_name)
        linkTextView = view.findViewById(R.id.text_view_custom_stream_url)
        userAgentTextView = view.findViewById(R.id.text_view_custom_stream_user_agent)
        headersTextView = view.findViewById(R.id.text_view_custom_stream_headers)
        acceptButton = view.findViewById(R.id.button_stream_confirm)
        editConfirmButton = view.findViewById(R.id.button_stream_edit)
        editCancelButton = view.findViewById(R.id.button_stream_cancel)
        headerTIL = view.findViewById(R.id.til_headers)
        userAgentTIL = view.findViewById(R.id.til_user_agent)
        titleTIL = view.findViewById(R.id.til_stream_name)
        optionalTV = view.findViewById(R.id.tv_optional)
        recyclerView = view.findViewById(R.id.recycler_m3u)
        initBottomSheet(view)
        if (fragmentType == 1) {
            hideView()
            m3UDisplayFragment = M3UDisplayFragment.newInstance()
        }
        return view
    }

    private fun historyItemClicked(playerModel: PlayerModel, isClicked: Boolean) {
        //update the bottom sheet
        playerModelNow = playerModel
        bottomSheetBehavior.isDraggable = playerModelNow.id != -1L
        if (playerModel.id != -1L) {
            val title = "${playerModel.title}\n${playerModel.link}"
            titleTextViewM3U.text = title
            if (isClicked)
                parseM3U(playerModelNow.link)
            else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } else {
            titleTextViewM3U.setText(R.string.bottom_sheet_header_none)
        }
    }

    private fun initBottomSheet(view: View) {
        bottomSheetLinearLayout = view.findViewById(R.id.linear_layout_bottom_sheet)
        titleTextViewM3U = view.findViewById(R.id.text_view_title_bs)
        playButton = view.findViewById(R.id.material_button_play_bs)
        editButton = view.findViewById(R.id.material_button_edit_bs)
        shareButton = view.findViewById(R.id.material_button_share_bs)
        deleteButton = view.findViewById(R.id.material_button_delete_bs)

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
//        playButtonLarge.setOnClickListener {
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            parseM3U(playerModelNow.link)
//        }
        playButton.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            parseM3U(playerModelNow.link)
        }
        editButton.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            acceptButton.visibility = View.GONE
            editCancelButton.visibility = View.VISIBLE
            editConfirmButton.visibility = View.VISIBLE
            linkTextView.setText(playerModelNow.link)
        }
        shareButton.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            val intentNow = PlayerUtils.createViewIntent(playerModelNow)
            if (intentNow.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intentNow)
            } else {
                toaster(requireActivity(), GlobalFunctions.NO_APP_FOUND_PLAY_MESSAGE)
            }

        }
        deleteButton.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            deleteModel(playerModelNow)
            playerModelNow = PlayerModel(-1)
            historyItemClicked(playerModelNow, false)
        }
        editConfirmButton.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            userInputM3U(false)
            acceptButton.visibility = View.VISIBLE
            editCancelButton.visibility = View.GONE
            editConfirmButton.visibility = View.GONE
            toaster(requireActivity(), "updated")
        }
        editCancelButton.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            acceptButton.visibility = View.VISIBLE
            editCancelButton.visibility = View.GONE
            editConfirmButton.visibility = View.GONE
            toaster(requireActivity(), "Cancelled")
        }
    }

    private fun deleteModel(playerModel: PlayerModel) {
        galleryAdapter.deleteData(playerModel)
        viewModel.deleteModel(playerModel)
    }

    private fun hideView() {
        headerTIL.visibility = View.GONE
        userAgentTIL.visibility = View.GONE
        //titleTIL.visibility = View.GONE
        //optionalTV.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        bottomSheetLinearLayout.visibility = View.VISIBLE
    }


    private fun startStreaming(data: ArrayList<PlayerModel>, selectedIdx: Int = 0) {
        PlayListAll.clear()
        PlayListAll.addAll(data)
        val intentNext = GlobalFunctions.getDefaultPlayer(
            requireContext(),
            mSettings,
            data[selectedIdx]
        )
        //val intentNext = Intent(requireActivity(), VlcActivity::class.java)
        intentNext.putExtra(PlayerModel.SELECTED_MODEL, selectedIdx)
        startActivity(intentNext)
    }

    private fun userInputLink() {
        val urlNow = linkTextView.text.toString()
        val titleNow: String =
            if (titleTextView.text == null || titleTextView.text.toString().isEmpty()) {
                GlobalFunctions.getNameFromUrl(urlNow)
            } else {
                titleTextView.text.toString()
            }
        val userAgentNow: String =
            if (userAgentTextView.text == null || userAgentTextView.text.toString()
                    .isEmpty()
            ) {
                GlobalFunctions.USER_AGENT
            } else {
                userAgentTextView.text.toString()
            }
        val headers: String =
            if (headersTextView.text == null || headersTextView.text.toString().isEmpty()) {
                ""
            } else {
                headersTextView.text.toString()
            }
        val model =
            PlayerModel(
                id = PlayerModel.getId(urlNow, titleNow),
                link = urlNow,
                userAgent = userAgentNow,
                title = titleNow
            )
        val headersPlayer = HashMap<String, String>()
        headersPlayer["User-Agent"] = userAgentNow
        if (headers.isNotEmpty()) {
            val headerNow = headers.split(",")
            var index = 1
            while (index < headerNow.size) {
                headersPlayer[headerNow[index - 1]] = headerNow[index]
                index += 2
            }
        }
        if (!headersPlayer["referer"].isNullOrEmpty()) {
            headersPlayer["referrer"] = headersPlayer["referer"] ?: ""
            headersPlayer["Referrer"] = headersPlayer["referer"] ?: ""
        }
        model.headers = headersPlayer

        SharedPreferenceUtils.saveUserLastInput(
            requireContext(),
            model.link,
            model.title,
            model.userAgent,
            headers
        )

        val data = ArrayList<PlayerModel>()
        data.add(model)
        startStreaming(data)
    }

    private fun m3uDataAction(playerModels: ArrayList<PlayerModel>) {
        PlayListAll.clear()
        PlayListAll.addAll(playerModels)
        requireView().findNavController().navigate(R.id.action_streamFragment_to_m3UDisplayFragment)
    }

    private fun parseM3U(urlNow: String) {
        val scrapper = Scrapper(requireContext(), urlNow)
        val loading = GlobalFunctions.createAlertDialogueLoading(requireActivity())
        scrapper.onFinish(object : OnScrappingCompleted {
            override fun onComplete(response: String) {
                loading.dismiss()
                val data = Parser.ParseM3UString(response, "User")
                if (data.isNotEmpty())
                    m3uDataAction(data)
                else
                    toaster(requireActivity(), "Empty List/Parsing Failed")
            }

            override fun onError() {
                loading.dismiss()
                toaster(requireActivity(), "Error Occurred")
            }
        })
        loading.show()
        scrapper.startScrapping()
    }

    private fun userInputM3U(insert: Boolean) {
        val urlNow = linkTextView.text.toString()

        toaster(requireActivity(), urlNow)
        val uri = Uri.parse(urlNow)

        val token =
            URLDecoder.decode(
                if (titleTextView.text != null && titleTextView.text.toString()
                        .isNotEmpty()
                ) {
                    titleTextView.text.toString()
                } else {
                    uri.lastPathSegment ?: "User M3U"
                }, "UTF-8"
            )

        SharedPreferenceUtils.setUserM3U(requireContext(), urlNow, token)
        val playerModel =
            PlayerModel(link = urlNow, streamType = PlayerModel.STREAM_M3U, title = token)
        playerModel.id = PlayerModel.getId(playerModel.link, playerModel.title)
        if (!insert) {
            println("deleting")
            //println(playerModelNow)
            deleteModel(playerModelNow)
        }
        println("inserting")
        //println(playerModel)
        viewModel.insertModel(playerModel)
        historyItemClicked(playerModel, false)
        //historyDatabase.playerModelDao().insertModel(playerModel)
        recyclerView.smoothScrollToPosition(galleryAdapter.addData(playerModel))
    }

    private fun linkChecker(): Boolean {
        if (linkTextView.text == null || linkTextView.text.toString()
                .isEmpty() || !Patterns.WEB_URL.matcher(
                linkTextView.text.toString()
            ).matches()
        ) {
            linkTextView.error = resources.getString(R.string.enter_stream_link)
            linkTextView.requestFocus()
            return false
        }
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettings = Settings(requireContext())
        if (fragmentType == 0) {
            val userPrevData = SharedPreferenceUtils.getUserLastInput(requireContext())
            linkTextView.setText(userPrevData[0])
            titleTextView.setText(userPrevData[1])
            userAgentTextView.setText(userPrevData[2])
            headersTextView.setText(userPrevData[3])
        } else {
            val userPrevData = SharedPreferenceUtils.getUserM3U(requireContext())
            linkTextView.setText(userPrevData[0])
            titleTextView.setText(userPrevData[1])
            //historyDatabase = HistoryDatabase.getInstance(requireContext())
            galleryAdapter = GalleryAdapter(GalleryItemViewHolder.M3U_LIST, { video, _ ->
                historyItemClicked(video, true)
                //parseM3U(video.link)
            }, { video, _ ->
                historyItemClicked(video, false)
            })
            recyclerView.also { viewR ->
                viewR.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                viewR.adapter = galleryAdapter
            }
            viewModel.videos.observe(viewLifecycleOwner, { videos ->
                galleryAdapter.updateData(videos)
                if (videos.isNotEmpty())
                    recyclerView.smoothScrollToPosition(videos.size - 1)
            })
            viewModel.getAllM3U()
            //val data = historyDatabase.playerModelDao().getAllM3U()
            //logger("here", "$data.size")
            //galleryAdapter.updateData(data)
        }
        acceptButton.setOnClickListener {
            if (linkChecker()) {
                if (fragmentType == 0) {
                    userInputLink()
                } else {
                    userInputM3U(true)
                }
            }
        }
    }


    companion object {
        const val ARG_TYPE = "FRAGMENT_TYPE"

        @JvmStatic
        fun newInstance(param: Int) =
            UserLinkFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TYPE, param)
                }
            }
    }
}