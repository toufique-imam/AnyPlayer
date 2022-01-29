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
import com.airbnb.lottie.LottieAnimationView
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.viewmodel.WebVideoViewModel
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.logger
import com.stream.jmxplayer.utils.GlobalFunctions.toaster
import com.stream.jmxplayer.utils.ijkplayer.Settings
import org.adblockplus.libadblockplus.android.webview.AdblockWebView
import java.io.ByteArrayInputStream
import java.net.URL
import java.net.URLConnection

class WebViewFragment : Fragment() {
    lateinit var webView: AdblockWebView
    var urlNow: String = ""
    val webVideoViewModel: WebVideoViewModel by viewModels()
    lateinit var fabWatch: FloatingActionButton
    lateinit var lottieAnimationView: LottieAnimationView
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
            "fragment_video_track_web"
        )
        webVideoDialogFragment.onBindInitiated = {
            webVideoDialogFragment.onWebVideoChanged()
        }

    }

    fun goBack(): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    fun goForward(): Boolean {
        return if (webView.canGoForward()) {
            webView.goForward()
            true
        } else {
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.toolbar_web, menu)
        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search)?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
        searchView.maxWidth = Int.MAX_VALUE
        searchView.queryHint = "Browse or Search"
        searchView.setOnSearchClickListener {
            searchView.setQuery(webView.url, false)
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                webAction(query)
                searchView.onActionViewCollapsed()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                toaster(requireActivity(), webView.url.toString())
                webView.reload()
                true
            }
            R.id.action_back -> {
                goBack()
            }
            R.id.action_forward -> {
                goForward()
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
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
        webView = view.findViewById(R.id.main_webview)

        lottieAnimationView = view.findViewById(R.id.lottie_loading)
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
        webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = true
        webView.settings.domStorageEnabled = true;

        webView.isLongClickable = true
        //
        webView.webViewClient = JMXWebClient()
        //zoom
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        //
        webView.settings.loadsImagesAutomatically = true
        webView.settings.javaScriptEnabled = true

        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.loadUrl("https://adblock-tester.com/")
    }

    inner class JMXWebClient : WebViewClient() {
//        private val emptyResponse = ByteArrayInputStream("".toByteArray())
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            try {
                val requestUrl = request?.url.toString()
                if (view != null && request != null) {
                    if (processUrl(requestUrl) || !getMimeUrl(requestUrl).isNullOrEmpty()) {
                        return super.shouldInterceptRequest(view, request)
                    }
                    try {
                        val urlNow = URL(requestUrl)
                        val urlConnection: URLConnection = urlNow.openConnection()
                        urlConnection.connectTimeout = 3000
                        urlConnection.connect()
                        val contentType = urlConnection.contentType
                        logger("type", contentType)
                        if (isVideo(contentType)) {
                            addUrlToModel(requestUrl)
                        }
                    } catch (e: Exception) {
                    }
                }
                return super.shouldInterceptRequest(view, request)
            } catch (e: Exception) {
                return null
            }
        }

        private fun isVideo(mimeType: String?): Boolean {
            if (mimeType == null) return false
            when (mimeType) {
                "application/octet-stream" -> return true
                MimeTypes.APPLICATION_M3U8 -> return true
                MimeTypes.APPLICATION_MPD -> return true
                MimeTypes.APPLICATION_SS -> return true
                MimeTypes.APPLICATION_MATROSKA -> return true
                MimeTypes.APPLICATION_MP4 -> return true
                MimeTypes.APPLICATION_MP4VTT -> return true
                MimeTypes.APPLICATION_WEBM -> return true
                else -> MimeTypes.isVideo(mimeType) or MimeTypes.isAudio(mimeType)
            }
            return MimeTypes.isVideo(mimeType)
        }

        private fun addUrlToModel(url: String) {
            val playerModel = PlayerModel()
            playerModel.link = url
            playerModel.title = Uri.parse(url).lastPathSegment + ""
            playerModel.mainLink = urlNow
            playerModel.addHeader("referer", urlNow)
            playerModel.id = PlayerModel.getId(playerModel.link, playerModel.title)
            webVideoViewModel.addDownloadModel(playerModel)
        }

        private fun getMimeUrl(url: String?): String? {
            if (url == null) return null
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }

        private fun processUrl(url: String?): Boolean {
            if (url == null) return true
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            return if (isVideo(mimeType) || extension.contains("m3u8")) {
                addUrlToModel(url)
                true
            } else {
                logger("process", "$url Extension: $extension Mime: $mimeType")
                false
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            webVideoViewModel.clearDownloadModel()
            if (url != null) {
                urlNow = url
            }
            lottieAnimationView.setVisible()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            lottieAnimationView.setGone()
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            lottieAnimationView.setGone()
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            lottieAnimationView.setGone()
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
            processUrl(url)
        }
    }

}