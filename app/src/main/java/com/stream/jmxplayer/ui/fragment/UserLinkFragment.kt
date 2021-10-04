package com.stream.jmxplayer.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.adapter.GalleryItemViewHolder
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.PlayerActivity
import com.stream.jmxplayer.ui.viewmodel.DatabaseViewModel
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.toaster
import com.stream.jmxplayer.utils.SharedPreferenceUtils
import com.stream.jmxplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.stream.jmxplayer.utils.m3u.OnScrappingCompleted
import com.stream.jmxplayer.utils.m3u.Parser
import com.stream.jmxplayer.utils.m3u.Scrapper

class UserLinkFragment : Fragment() {
    lateinit var titleTextView: TextInputEditText
    lateinit var linkTextView: TextInputEditText
    lateinit var userAgentTextView: TextInputEditText
    lateinit var headersTextView: TextInputEditText
    lateinit var acceptButton: MaterialButton

    lateinit var headerTIL: TextInputLayout
    lateinit var userAgentTIL: TextInputLayout
    lateinit var titleTIL: TextInputLayout
    lateinit var optionalTV: TextView

    lateinit var recyclerView: RecyclerView

    private val viewModel: DatabaseViewModel by viewModels()

    //private lateinit var historyDatabase: HistoryDatabase
    lateinit var galleryAdapter: GalleryAdapter
    lateinit var m3UDisplayFragment: M3UDisplayFragment

    var fragmentType: Int = 0

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
        headerTIL = view.findViewById(R.id.til_headers)
        userAgentTIL = view.findViewById(R.id.til_user_agent)
        titleTIL = view.findViewById(R.id.til_stream_name)
        optionalTV = view.findViewById(R.id.tv_optional)
        recyclerView = view.findViewById(R.id.recycler_m3u)
        if (fragmentType == 1) {
            hideView()
            m3UDisplayFragment = M3UDisplayFragment.newInstance()
        }
        return view
    }

    private fun hideView() {
        headerTIL.visibility = View.GONE
        userAgentTIL.visibility = View.GONE
        titleTIL.visibility = View.GONE
        optionalTV.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }


    private fun startStreaming(data: ArrayList<PlayerModel>, selectedIdx: Int = 0) {
        PlayListAll.clear()
        PlayListAll.addAll(data)
        val intentNext = Intent(requireActivity(), PlayerActivity::class.java)
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
        if (headers.isNotEmpty()) {
            val headerNow = headers.split(",")
            var index = 1
            val headersPlayer = HashMap<String, String>()
            while (index < headerNow.size) {
                headersPlayer[headerNow[index - 1]] = headerNow[index]
                index += 2
            }
            model.headers = headersPlayer
        }
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

    private fun userInputM3U() {
        val urlNow = linkTextView.text.toString()
        toaster(requireActivity(), urlNow)
        SharedPreferenceUtils.setUserM3U(requireContext(), urlNow)
        val uri = Uri.parse(urlNow)
        val token = uri.lastPathSegment ?: "User M3U"
        val playerModel =
            PlayerModel(link = urlNow, streamType = PlayerModel.STREAM_M3U, title = token)
        playerModel.id = PlayerModel.getId(playerModel.link, playerModel.title)
        viewModel.insertModel(playerModel)
        //historyDatabase.playerModelDao().insertModel(playerModel)
        recyclerView.smoothScrollToPosition(galleryAdapter.addData(playerModel))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (fragmentType == 0) {
            val userPrevData = SharedPreferenceUtils.getUserLastInput(requireContext())
            linkTextView.setText(userPrevData[0])
            titleTextView.setText(userPrevData[1])
            userAgentTextView.setText(userPrevData[2])
            headersTextView.setText(userPrevData[3])
        } else {
            val userPrevData = SharedPreferenceUtils.getUserM3U(requireContext())
            linkTextView.setText(userPrevData)
            //historyDatabase = HistoryDatabase.getInstance(requireContext())
            galleryAdapter = GalleryAdapter(GalleryItemViewHolder.M3U_LIST, { video, _ ->
                parseM3U(video.link)
            }, { _, _ -> })
            recyclerView.also { viewR ->
                viewR.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
                viewR.adapter = galleryAdapter
            }
            viewModel.videos.observe(viewLifecycleOwner, { videos ->
                galleryAdapter.updateData(videos)
            })
            viewModel.getAllM3U()
            //val data = historyDatabase.playerModelDao().getAllM3U()
            //logger("here", "$data.size")
            //galleryAdapter.updateData(data)
        }
        acceptButton.setOnClickListener {
            if (linkTextView.text == null || linkTextView.text.toString()
                    .isEmpty() || !Patterns.WEB_URL.matcher(
                    linkTextView.text.toString()
                ).matches()
            ) {
                linkTextView.error = resources.getString(R.string.enter_stream_link)
                linkTextView.requestFocus()
            } else {
                if (fragmentType == 0) {
                    userInputLink()
                } else {
                    userInputM3U()
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