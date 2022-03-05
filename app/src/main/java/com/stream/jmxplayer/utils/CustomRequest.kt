package com.stream.jmxplayer.utils


import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import java.nio.charset.StandardCharsets

class CustomRequest(
    method: Int,
    url: String,
    private val mHeaders: HashMap<String, String>,
    private val listener: Response.Listener<String>,
    errorListener: Response.ErrorListener
) : Request<String>(method, url, errorListener) {


    override fun getHeaders(): Map<String, String> {
        return mHeaders
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
        return try {
            val json = String(
                response?.data ?: ByteArray(0),
                StandardCharsets.UTF_8
            )
            Response.success(json, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            Response.error(VolleyError(e))
        }
    }


    override fun deliverResponse(response: String?) {
        listener.onResponse(response)
    }
}