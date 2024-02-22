package com.retroline.anyplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.retroline.anyplayer.R
import com.retroline.anyplayer.model.PlayerModel
import com.retroline.anyplayer.utils.GlobalFunctions
import com.retroline.anyplayer.utils.Settings
import com.retroline.anyplayer.utils.SharedPreferenceUtils
import com.retroline.anyplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.retroline.anyplayer.utils.checkUrl

class UserLinkFragment : Fragment() {
    private lateinit var titleTextView: TextInputEditText
    private lateinit var linkTextView: TextInputEditText
    private lateinit var userAgentTextView: TextInputEditText
    private lateinit var headersTextView: TextInputEditText

    private lateinit var acceptButton: MaterialButton

    private lateinit var headerTIL: TextInputLayout
    private lateinit var userAgentTIL: TextInputLayout
    private lateinit var titleTIL: TextInputLayout
    private lateinit var optionalTV: TextView

    lateinit var mSettings: Settings


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

        return view
    }


    private fun startStreaming(data: ArrayList<PlayerModel>, selectedIdx: Int = 0) {
        PlayListAll.clear()
        PlayListAll.addAll(data)
        val intentNext = GlobalFunctions.getDefaultPlayer(
            requireContext(),
            mSettings,
            data[selectedIdx]
        )
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
                id = PlayerModel.getId(urlNow, titleNow, PlayerModel.STREAM_ONLINE_GENERAL),
                link = urlNow,
                userAgent = userAgentNow,
                title = titleNow,
                streamType = PlayerModel.STREAM_ONLINE_GENERAL
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

    private fun linkChecker(): Boolean {
        if (!linkTextView.checkUrl()) {
            linkTextView.error = resources.getString(R.string.enter_stream_link)
            linkTextView.requestFocus()
            return false
        }
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettings = Settings(requireContext())
        val userPrevData = SharedPreferenceUtils.getUserLastInput(requireContext())
        linkTextView.setText(userPrevData[0])
        titleTextView.setText(userPrevData[1])
        userAgentTextView.setText(userPrevData[2])
        headersTextView.setText(userPrevData[3])

        acceptButton.setOnClickListener {
            if (linkChecker()) {
                userInputLink()
            }
        }
    }


    companion object {
        @JvmStatic
        fun newInstance() =
            UserLinkFragment()
    }
}