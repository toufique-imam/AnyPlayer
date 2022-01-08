package com.stream.jmxplayer.ui.fragment

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.*
import android.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.viewmodel.WebVideoViewModel
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.GlobalFunctions.isProVersion
import com.stream.jmxplayer.utils.GlobalFunctions.logger
import com.stream.jmxplayer.utils.SharedPreferenceUtils
import com.stream.jmxplayer.utils.checkUrl
import com.stream.jmxplayer.utils.ijkplayer.Settings
import com.stream.jmxplayer.utils.isStarted


class WebViewFragment : Fragment() {
    lateinit var webView: WebView
    var urlNow: String = ""
    val webVideoViewModel: WebVideoViewModel by viewModels()
    lateinit var fabWatch: FloatingActionButton
    private lateinit var webVideoDialogFragment: WebVideoDialogFragment
    val mSettings: Settings by lazy {
        Settings(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun showVideoTrack() {
        if (!isStarted()) return
        if (webVideoDialogFragment.isAdded) return
        webVideoDialogFragment.arguments = bundleOf()
        webVideoDialogFragment.show(
            childFragmentManager,
            "fragment_video_track"
        )
        webVideoDialogFragment.onBindInitiated = {
            webVideoDialogFragment.onWebVideoChanged()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        if (isProVersion()) {
            inflater.inflate(R.menu.toolbar_browse_pro, menu)
        } else {
            inflater.inflate(R.menu.toolbar_browse, menu)
        }
        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search)?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
        searchView.maxWidth = Int.MAX_VALUE
        searchView.queryHint = "Browse or Search"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                webAction(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_refresh) {
            webView.reload()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.stopLoading()
        webView.loadUrl("")
        webView.reload()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    fun webAction(queryString: String?) {
        if (queryString == null) return
        if (queryString.isNotEmpty()) {
            if (queryString.checkUrl()) {
                urlNow = queryString
                if (!queryString.startsWith("https://") && !queryString.startsWith("http://")) {
                    urlNow = "https://$queryString"
                }
            } else {
                urlNow = "https://www.google.com/search?q=$queryString"
            }
            logger("URL", urlNow)
            webView.loadUrl(urlNow)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //init views
        webView = view.findViewById(R.id.webview)
        fabWatch = view.findViewById(R.id.fab_web_video_list)
        webVideoDialogFragment = WebVideoDialogFragment({ playerModel, _ ->
            val intent = GlobalFunctions.getDefaultPlayer(requireContext(), mSettings, playerModel)
            SharedPreferenceUtils.PlayListAll.clear()
            SharedPreferenceUtils.PlayListAll.add(playerModel)
            startActivity(intent)
        }, webVideoViewModel)
        fabWatch.setOnClickListener {
            showVideoTrack()
        }
        webView.settings.loadsImagesAutomatically = true
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.builtInZoomControls = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(true)

        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.webViewClient = JMXWebClient()
        webView.loadUrl("https://www.google.com")
    }

    inner class JMXWebClient : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val requestUrl = request?.url.toString()
            processUrl(requestUrl)
            return super.shouldInterceptRequest(view, request)
        }

        override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
            processUrl(url)
            return super.shouldInterceptRequest(view, url)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            processUrl(url)
            return false
        }

        override fun onLoadResource(view: WebView?, url: String?) {

            processUrl(url)

        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url?.toString()
            processUrl(url)
            return false
        }

        private fun isVideo(mimeType: String?): Boolean {
            if (mimeType == null) return false
            when (mimeType) {
                MimeTypes.APPLICATION_M3U8 -> return true
                MimeTypes.APPLICATION_MPD -> return true
                MimeTypes.APPLICATION_SS -> return true
                else -> MimeTypes.isVideo(mimeType)
            }
            return MimeTypes.isVideo(mimeType)
        }

        private fun processUrl(url: String?) {
            if (url == null) return
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

            if (isVideo(mimeType)) {
                val playerModel = PlayerModel()
                playerModel.link = url
                playerModel.title = Uri.parse(url).lastPathSegment + ""
                playerModel.id = PlayerModel.getId(playerModel.link, playerModel.title)
                webVideoViewModel.addDownloadModel(playerModel)
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            webVideoViewModel.clearDownloadModel()
        }
    }

}