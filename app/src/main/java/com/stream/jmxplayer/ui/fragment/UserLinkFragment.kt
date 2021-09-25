package com.stream.jmxplayer.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.stream.jmxplayer.R
import com.stream.jmxplayer.adapter.GalleryAdapter
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.model.db.SharedPreferenceUtils
import com.stream.jmxplayer.model.db.SharedPreferenceUtils.Companion.PlayListAll
import com.stream.jmxplayer.ui.PlayerActivity
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.m3u.OnScrappingCompleted
import com.stream.jmxplayer.utils.m3u.Parser
import com.stream.jmxplayer.utils.m3u.Scrapper

class UserLinkFragment : Fragment() {
    lateinit var titleTextView: TextInputEditText
    lateinit var linkTextView: TextInputEditText
    lateinit var userAgentTextView: TextInputEditText
    lateinit var headersTextView: TextInputEditText
    lateinit var acceptButton: MaterialButton
    lateinit var recyclerView: RecyclerView
    lateinit var fabPlay: FloatingActionButton

    lateinit var headerTIL: TextInputLayout
    lateinit var userAgentTIL: TextInputLayout
    lateinit var titleTIL: TextInputLayout
    lateinit var optionalTV: TextView

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
        fabPlay = view.findViewById(R.id.fab_play)
        if (fragmentType == 1) {
            hideView()
        }
        return view
    }

    private fun hideView() {
        headerTIL.visibility = View.GONE
        userAgentTIL.visibility = View.GONE
        titleTIL.visibility = View.GONE
        optionalTV.visibility = View.GONE
    }


    private fun startStreaming(data: ArrayList<PlayerModel>, selectedIdx: Int = 0) {
        PlayListAll.clear()
        PlayListAll.addAll(data)
        val intentNext = Intent(requireActivity(), PlayerActivity::class.java)
        //intentNext.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        //intentNext.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        //intentNext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        //intentNext.putExtra(PlayerModel.DIRECT_PUT, data)
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
            val headers = HashMap<String, String>()
            while (index < headerNow.size) {
                headers[headerNow[index - 1]] = headerNow[index]
                index += 2
            }
            model.headers = headers
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
        val galleryAdapter = GalleryAdapter(
            0,
            { _, pos -> startStreaming(data = playerModels, pos) },
            { _, _ -> })
        galleryAdapter.setHasStableIds(true)
        galleryAdapter.updateData(data = playerModels)
        val spanCount = GlobalFunctions.getGridSpanCount(requireActivity())

        recyclerView.also { viewR ->
            viewR.layoutManager = GridLayoutManager(context, spanCount)
            viewR.adapter = galleryAdapter
        }
        fabPlay.setOnClickListener {
            startStreaming(data = playerModels)
        }
        recyclerView.visibility = View.VISIBLE
        fabPlay.visibility = View.VISIBLE
    }

    private fun userInputM3U() {
        val urlNow = linkTextView.text.toString()
        SharedPreferenceUtils.setUserM3U(requireContext(), urlNow)
        val scrapper = Scrapper(requireContext(), urlNow)
        val loading = GlobalFunctions.createAlertDialogueLoading(requireActivity())
        recyclerView.visibility = View.GONE
        fabPlay.visibility = View.GONE
        scrapper.onFinish(object : OnScrappingCompleted {
            override fun onComplete(response: String) {
                loading.dismiss()
                //logger("Parsing", response)
                val data = Parser.ParseM3UString(response, "User")
                //startStreaming(data)
                if (data.isNotEmpty())
                    m3uDataAction(data)
                else
                    GlobalFunctions.toaster(requireActivity(), "Empty List/Parsing Failed")
            }

            override fun onError() {
                loading.dismiss()
                GlobalFunctions.toaster(requireActivity(), "Error Occurred")
            }
        })
        loading.show()
        scrapper.startScrapping()
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

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @param param Fragment Type.
         * @return A new instance of fragment UserLinkFragment.
         */
        @JvmStatic
        fun newInstance(param: Int) =
            UserLinkFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TYPE, param)
                }
            }
    }
}