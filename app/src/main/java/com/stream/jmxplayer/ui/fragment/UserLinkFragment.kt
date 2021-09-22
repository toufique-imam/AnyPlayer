package com.stream.jmxplayer.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.PlayerActivity
import com.stream.jmxplayer.utils.GlobalFunctions

class UserLinkFragment : Fragment() {
    lateinit var titleTextView: TextInputEditText
    lateinit var linkTextView: TextInputEditText
    lateinit var userAgentTextView: TextInputEditText
    lateinit var headersTextView: TextInputEditText
    lateinit var acceptButton: MaterialButton

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
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        acceptButton.setOnClickListener {
            if (linkTextView.text == null || linkTextView.text.toString()
                    .isEmpty() || !Patterns.WEB_URL.matcher(
                    linkTextView.text.toString()
                ).matches()
            ) {
                linkTextView.error = resources.getString(R.string.enter_stream_link)
                linkTextView.requestFocus()
            } else {
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
                val intentNext = Intent(requireActivity(), PlayerActivity::class.java)
                //  intentNext.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intentNext.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intentNext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intentNext.putExtra(PlayerModel.DIRECT_PUT, model)
                startActivity(intentNext)
            }
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment UserLinkFragment.
         */
        @JvmStatic
        fun newInstance() =
            UserLinkFragment()
    }
}