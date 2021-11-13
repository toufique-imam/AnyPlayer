package com.stream.jmxplayer.utils


import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
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
            logger("customReq", HttpHeaderParser.parseCharset(response?.headers))
            val json = String(
                response?.data ?: ByteArray(0),
                StandardCharsets.UTF_8
//                Charset.forName(HttpHeaderParser.parseCharset(response?.headers))
            )
            Response.success(json, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            Response.error(VolleyError(e))
        }
    }

//    private fun fromHtml(html: String): Spanned? {
//        val htmlNew = (html.replace("&lt;", "<").replace("&gt;", ">"))
//        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            Html.fromHtml(html.trim(), Html.FROM_HTML_MODE_LEGACY)
//        } else Html.fromHtml(html.trim())
//        return result
//    }

    override fun deliverResponse(response: String?) {
        listener.onResponse(response)
    }
}