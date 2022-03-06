package com.stream.jmxplayer.ui.fragment

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.BrowserActivity
import com.stream.jmxplayer.ui.viewmodel.WebVideoViewModel
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.logger
import com.stream.jmxplayer.utils.GlobalFunctions.toaster
import java.net.URL
import java.net.URLConnection
import java.util.*


class WebViewFragment : Fragment() {
    private lateinit var webView: WebView
    var urlNow: String = ""
    private lateinit var fabWatch: FloatingActionButton
    private lateinit var fabDesktop: FloatingActionButton
    private lateinit var webVideoDialogFragment: WebVideoDialogFragment
    private lateinit var searchView: SearchView
    private lateinit var linearProgressIndicator: LinearProgressIndicator
    var isLoading: Boolean = false

    val webVideoViewModel: WebVideoViewModel by viewModels()
    val mSettings: Settings by lazy {
        Settings(requireContext())
    }
    val adBlocker: AdBlocker by lazy {
        AdBlocker.getInstance(requireActivity().applicationContext)
    }

    fun shakeFab() =
        fabWatch.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.anim_shake))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun showVideoTrack() {
        if (!isStarted()) {
            return
        }
        if (webVideoDialogFragment.isVisible) {
            webVideoDialogFragment.webVideoViewModel.getDownloadData()
            return
        }
        if (webVideoDialogFragment.isAdded) {
            childFragmentManager.beginTransaction().remove(webVideoDialogFragment).commit()
        }
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

    private fun goForward(): Boolean {
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
        searchView = menu.findItem(R.id.action_search)?.actionView as SearchView
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
        webVideoViewModel.clearDownloadModel()
        if (queryString.isNotEmpty()) {
            if (queryString.checkUrl()) {
                urlNow = queryString
                if (!queryString.startsWith("https://") && !queryString.startsWith("http://")) {
                    urlNow = "https://$queryString"
                }
            } else {
                urlNow = "https://www.google.com/search?q=$queryString"
            }
            webView.stopLoading()
            if (!isLoading) {
                linearProgressIndicator.isIndeterminate = true
                linearProgressIndicator.show()
            }
            webView.loadUrl(urlNow)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //init views
        webView = view.findViewById(R.id.main_webview)
        fabWatch = view.findViewById(R.id.fab_web_video_list)
        fabDesktop = view.findViewById(R.id.fab_web_pc)
        linearProgressIndicator = view.findViewById(R.id.progress_indicator)
//        linearProgressIndicator.isIndeterminate = true
        webVideoDialogFragment = WebVideoDialogFragment(webVideoViewModel) { playerModel, _ ->
            webVideoDialogFragment.dismiss()
            val intent = GlobalFunctions.getDefaultPlayer(requireContext(), mSettings, playerModel)
            SharedPreferenceUtils.PlayListAll.clear()
            SharedPreferenceUtils.PlayListAll.add(playerModel)
            startActivity(intent)
        }
        fabWatch.setOnClickListener {
            //toaster(requireActivity(), "fab clicked")
            if (requireActivity() is BrowserActivity) {
                (requireActivity() as BrowserActivity).loadAd(0) {
                    showVideoTrack()
                }
            }
        }
        fabDesktop.setOnClickListener {
            val type = fabDesktop.contentDescription.toString()
            if (type == getString(R.string.web_type_phone)) {
                fabDesktop.contentDescription = getString(R.string.web_type_desktop)
                fabDesktop.setImageResource(R.drawable.ic_baseline_desktop_windows_24)
                updateWebViewSettings(true)
            } else {
                fabDesktop.contentDescription = getString(R.string.web_type_phone)
                fabDesktop.setImageResource(R.drawable.ic_baseline_smartphone_24)
                updateWebViewSettings(false)
            }
        }
        initWebViewSettings()
//        val testUrl = "https://mixdrop.co/e/mdwkjd39b43wx"
        val testUrl = "https://ustv247.tv/cartoon-network"
        val landingUrl = "https://google.com"
        webView.loadUrl(testUrl)
    }

    private fun updateWebViewSettings(isDesktop: Boolean) {
        if (isDesktop) {
            webView.settings.loadWithOverviewMode = true
            webView.settings.useWideViewPort = true
            webView.isScrollbarFadingEnabled = false
            webView.settings.userAgentString = GlobalFunctions.USER_AGENT
        } else {
            webView.isScrollbarFadingEnabled = true
            webView.settings.userAgentString = WebSettings.getDefaultUserAgent(context)
        }
        webView.reload()
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initWebViewSettings() {
        webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY

        webView.isScrollbarFadingEnabled = true
        webView.settings.domStorageEnabled = true

        webView.isLongClickable = true
        //
        webView.webViewClient = JMXWebClient()
        //zoom
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.settings.setSupportMultipleWindows(true)
        //
        webView.settings.loadsImagesAutomatically = true
        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                linearProgressIndicator.setProgressCompat(newProgress, true)
                when (newProgress) {
                    100 -> {
                        isLoading = false
                        linearProgressIndicator.hide()
                    }
                    else -> {
                        isLoading = true
                        linearProgressIndicator.show()
                    }
                }
            }
        }

        webView.clearCache(true)
    }

    inner class JMXWebClient : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            try {
                val requestUrl = request?.url.toString()
                if (mSettings.adBlocked && adBlocker.isAd(requestUrl)) {
                    logger("isAd" , requestUrl)
                    return createEmptyResource()
                }
                if (view != null && request != null) {
                    if (checkHeader(request)) {
                        return super.shouldInterceptRequest(view, request)
                    }
                    if (processUrl(request)) {
                        return super.shouldInterceptRequest(view, request)
                    }
                    if (!request.method.equals("POST")) {
                        try {
                            val urlNow = URL(requestUrl)
                            val urlConnection: URLConnection = urlNow.openConnection()
                            for (key in request.requestHeaders) {
                                urlConnection.setRequestProperty(key.key, key.value)
                            }
                            urlConnection.connectTimeout = 3000
                            urlConnection.connect()
                            val contentType = urlConnection.contentType
                            if (isVideo(contentType)) {
                                addUrlToModel(requestUrl, request.requestHeaders)
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            } catch (e: Exception) {
                return createEmptyResource()
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

        private fun addUrlToModel(url: String, headers: Map<String, String>?) {
            val playerModel = PlayerModel()
            playerModel.link = url
            playerModel.title = Uri.parse(url).lastPathSegment + ""
            playerModel.mainLink = urlNow
            playerModel.addHeader("referer", urlNow)
            playerModel.streamType = PlayerModel.WEB_VIDEO
            if (headers != null) {
                for (key in headers) {
                    playerModel.addHeader(key.key, key.value)
                }
            }
            playerModel.id =
                PlayerModel.getId(playerModel.link, playerModel.title, playerModel.streamType)
            if (webVideoViewModel.addDownloadModel(playerModel)) {
                shakeFab()
            }
        }

        private fun createEmptyResource(): WebResourceResponse {
            return WebResourceResponse(
                "text/plain",
                "utf-8",
                null
            )
        }

        private fun checkHeader(request: WebResourceRequest): Boolean {
            val requestUrl = request.url.toString()

            for (key in request.requestHeaders) {
                if ((key.value.lowercase(Locale.getDefault()) == "video") || (key.value.lowercase(
                        Locale.getDefault()
                    ) == "audio")
                ) {
                    addUrlToModel(requestUrl, request.requestHeaders)
                    return true
                }
            }
            if ((request.requestHeaders["Sec-Fetch-Dest"]?.equals("video") == true) || (request.requestHeaders["sec-fetch-dest"]?.equals(
                    "video"
                ) == true)
            ) {
                addUrlToModel(requestUrl, request.requestHeaders)
                return true
            }
            if ((request.requestHeaders["Sec-Fetch-Dest"]?.equals("audio") == true) || (request.requestHeaders["sec-fetch-dest"]?.equals(
                    "audio"
                ) == true)
            ) {
                addUrlToModel(requestUrl, request.requestHeaders)
                return true
            }
            return false
        }

        private fun processUrl(url: String?): Boolean {
            if (url == null) return true
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            return if (isVideo(mimeType) || extension.contains("m3u8")) {
                addUrlToModel(url, null)
                true
            } else {
                false
            }
        }

        private fun processUrl(request: WebResourceRequest): Boolean {
            val url = request.url ?: return true
            val extension = MimeTypeMap.getFileExtensionFromUrl(url.toString())
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            return if (isVideo(mimeType) || extension.contains("m3u8")) {
                addUrlToModel(url.toString(), request.requestHeaders)
                true
            } else !extension.isNullOrEmpty() || !mimeType.isNullOrEmpty()
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (!isLoading) {
                linearProgressIndicator.isIndeterminate = true
                linearProgressIndicator.show()
            }
            if (url != null) {
                urlNow = url
            }
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
            processUrl(url)
        }

    }

}