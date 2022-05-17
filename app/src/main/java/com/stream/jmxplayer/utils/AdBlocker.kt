package com.stream.jmxplayer.utils

import android.content.Context
import com.stream.jmxplayer.model.db.SingletonHolder
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.buffer
import okio.source


class AdBlocker(val context: Context) {
    private val AD_HOSTS = HashSet<String>()
    private val detectedAdUrls = HashSet<String>()

    //    val AD_HOSTS_FILE1 = "pgl.yoyo.org.txt"
    val AD_HOSTS_FILE1 = "hosts"
//    val AD_HOSTS_FILE3 = "hosts2"

    init {
        readAdServers()
    }

    companion object : SingletonHolder<AdBlocker, Context>({
        AdBlocker(it.applicationContext)
    })

    private fun readAdServers() {
        val stream = context.assets.open(AD_HOSTS_FILE1)
        val buffer = stream.source().buffer()
        var line: String?
        while (true) {
            line = buffer.readUtf8Line()
            if (line == null) break
            AD_HOSTS.add(line)
        }
        buffer.close()
        stream.close()

    }

    private fun isAdHost(host: String): Boolean {
        if (host.isEmpty()) return false
        val index = host.indexOf(".")
        return index >= 0 && (AD_HOSTS.contains(host) || (index + 1 < host.length && isAdHost(
            host.substring(index + 1)
        )))
    }

    fun isAd(url: String?): Boolean {
        if (url == null) return false
        if (detectedAdUrls.contains(url)) return true
        val httpUrl = url.toHttpUrlOrNull() ?: return false
        return if (isAdHost(httpUrl.host)) {
            detectedAdUrls.add(url)
            true
        } else false
    }
    /*
        private fun readAdServers2() {
        val stream = context.assets.open(AD_HOSTS_FILE2)
        val buffer = stream.source().buffer()
        var line: String? = ""
        while (true) {
            line = buffer.readUtf8Line()
            if (line == null) break
            //trackmysales.com:99.63.194.183-99.63.194.183
            val data = line.split(":")
            if (data.isNotEmpty())
                AD_HOSTS.add(data[0])
        }
        buffer.close()
        stream.close()
    }

    private fun readAdServers3() {
        val stream = context.assets.open(AD_HOSTS_FILE3)
        val buffer = stream.source().buffer()
        var line: String? = ""
        while (true) {
            line = buffer.readUtf8Line()
            if (line == null) break
            //trackmysales.com:99.63.194.183-99.63.194.183
            val data = line.split(" ")
            if (data.size > 1)
                AD_HOSTS.add(data[1])
        }
        buffer.close()
        stream.close()
    }
     */
}