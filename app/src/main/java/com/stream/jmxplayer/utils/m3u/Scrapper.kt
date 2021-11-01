package com.stream.jmxplayer.utils.m3u

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley
import com.stream.jmxplayer.utils.CustomRequest
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.TextFileUtils
import java.net.HttpURLConnection
import java.net.URL

class Scrapper(val context: Context, private var url: String) {
    lateinit var onComplete: OnScrappingCompleted
    var mRequestQueue: RequestQueue? = null
    var textFileUtils = TextFileUtils(context)

    fun updateUrl(url: String) {
        this.url = url
    }

    fun deletePrevious() {
        textFileUtils.saveM3UFile(url, "")
    }

    fun startScrapping() {
        val savedData = textFileUtils.getSavedM3UFile(url)
        if (savedData.isNotEmpty()) {
            onComplete.onComplete(savedData)
            return
        }
        val headers = HashMap<String, String>()
        headers["user-agent"] = GlobalFunctions.USER_AGENT
        val localData = CustomRequest(
            Request.Method.GET, url, headers,
            { response ->
                textFileUtils.saveM3UFile(url, response)
                onComplete.onComplete(response)
            },
            { onComplete.onError() }
        )
        addToRequestQueue(localData)
    }


    private fun <T> addToRequestQueue(req: Request<T>) {
        req.tag = TAG
        getRequestQueue().add(req)
    }

    private fun getRequestQueue(): RequestQueue {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(this.context, object : HurlStack() {
                override fun createConnection(url: URL?): HttpURLConnection {
                    val connection = super.createConnection(url)
                    connection.instanceFollowRedirects = true
                    return connection
                }
            })
        }
        return mRequestQueue!!
    }

    fun onFinish(onScrappingCompleted: OnScrappingCompleted) {
        onComplete = onScrappingCompleted
    }

    companion object {
        private const val TAG = "APP"
    }
}

interface OnScrappingCompleted {
    fun onComplete(response: String)
    fun onError()
}