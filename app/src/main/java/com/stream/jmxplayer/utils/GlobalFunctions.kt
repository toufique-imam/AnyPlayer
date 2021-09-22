package com.stream.jmxplayer.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.stream.jmxplayer.R
import me.drakeet.support.toast.ToastCompat
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.NetworkInterface
import java.net.URL
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class GlobalFunctions {
    companion object {
        const val DEFAULT_STREAM_NAME = "JMX Stream"
        fun getNameFromUrl(url: String): String {
            if (url.isEmpty()) return DEFAULT_STREAM_NAME
            try {
                val resource = URL(url)
                val host = resource.host
                if (host.isNotEmpty() && url.endsWith(host)) {
                    return DEFAULT_STREAM_NAME
                }
            } catch (e: MalformedURLException) {
                return DEFAULT_STREAM_NAME
            }
            val startIndex = url.lastIndexOf('/') + 1
            val len = url.length
            var lastQMpos = url.lastIndexOf('?')
            if (lastQMpos == -1) {
                lastQMpos = len
            }
            var lastHashPos = url.lastIndexOf('#')
            if (lastHashPos == -1) lastHashPos = len
            val endIndex = lastHashPos.coerceAtMost(lastQMpos)
            return url.substring(startIndex, endIndex)
        }

        fun getGridSpanCount(activity: Activity): Int {
            val view: View = activity.layoutInflater.inflate(R.layout.gallery_item, null)
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val size = ceil(view.measuredWidth / 100.0) * 100
            val xInches = (getScreenWidth(activity) / size).toInt()
            return min(10, max(2, xInches))
        }

        private fun processAddress(useIPv4: Boolean, address: InetAddress): String? {
            val hostAddress = address.hostAddress
            val isIPv4 = hostAddress.indexOf(':') < 0
            if (useIPv4) {
                if (isIPv4) {
                    return hostAddress
                }
            } else {
                if (!isIPv4) {
                    val endIndex = hostAddress.indexOf('%') // drop ip6 zone suffix
                    return if (endIndex < 0) {
                        hostAddress.toUpperCase()
                    } else {
                        hostAddress.substring(0, endIndex).toUpperCase()
                    }
                }
            }
            return null
        }

        fun getIPAddress(useIPv4: Boolean): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                interfaces.forEach { netInterface ->
                    val addresses = netInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress) {
                            val result = processAddress(useIPv4, address)
                            if (result != null) {
                                Log.e("IPAddress", result)
                                return result
                            }
                        }
                    }
                }
            } catch (e: Exception) {

            }
            return "0.0.0.0"
        }

        fun toaster(activity: Activity, message: String) {
            ToastCompat.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }

        fun logger(TAG: String, message: String?) {
            Log.e(TAG, message ?: "Message")
        }

        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36"


        const val CHANGE_ORIENTATION_MESSAGE: String = "Orientacion de pantalla"
        const val DOWNLOAD_MESSAGE: String = "Guardar en el dispositivo"
        const val NO_APP_FOUND_PLAY_MESSAGE: String =
            "No se encontró ninguna aplicación para abrir este archivo"

        fun getScreenWidth(activity: Activity): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val insets = windowMetrics.windowInsets
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                windowMetrics.bounds.width() - insets.left - insets.right
            } else {
                val displayMetrics = DisplayMetrics()
                activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
                displayMetrics.widthPixels
            }
        }

        fun getScreenHeight(activity: Activity): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val insets = windowMetrics.windowInsets
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                windowMetrics.bounds.height() - insets.left - insets.right
            } else {
                val displayMetrics = DisplayMetrics()
                activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
                displayMetrics.heightPixels
            }
        }

        fun appInstalledOrNot(activity: Activity, str: String?): Boolean {
            return try {
                activity.packageManager.getPackageInfo(str!!, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        fun createAlertDialogueLoading(activity: Activity): AlertDialog {
            val dialogueView =
                activity.layoutInflater.inflate(R.layout.custom_loading_dialogue, null)
            return AlertDialog.Builder(activity)
                .setView(dialogueView)
                .setCancelable(false)
                .create()
        }

        fun milliSecondToString(duration: Int): String {
            val seconds = (duration / (1000)) % 60
            val minutes = (duration / (60 * 1000)) % 60
            val hours = (duration / (1000 * 60 * 60)) % 24
            val ans = StringBuilder()
            when {
                hours > 9 -> {
                    ans.append(hours)
                }
                hours > 0 -> {
                    ans.append("0")
                    ans.append(hours)
                }
                else -> {
                    ans.append("00")
                }
            }
            ans.append(":")

            when {
                minutes > 9 -> {
                    ans.append(minutes)
                }
                minutes > 0 -> {
                    ans.append("0")
                    ans.append(minutes)
                }
                else -> {
                    ans.append("00")
                }
            }
            ans.append(":")
            when {
                seconds > 9 -> {
                    ans.append(seconds)
                }
                seconds > 0 -> {
                    ans.append("0")
                    ans.append(seconds)
                }
                else -> {
                    ans.append("00")
                }
            }
            return ans.toString()
        }
    }
}