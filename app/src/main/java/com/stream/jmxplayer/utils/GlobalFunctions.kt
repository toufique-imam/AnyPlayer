package com.stream.jmxplayer.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.stream.jmxplayer.R
import me.drakeet.support.toast.ToastCompat
import java.net.NetworkInterface
import java.util.*

class GlobalFunctions {
    companion object {
        fun getIpAddress(): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val address = Collections.list(intf.inetAddresses)
                    for (addr in address) {
                        if (addr.isLoopbackAddress) {
                            val sAddr = addr.hostAddress
                            val isIPv4 = sAddr.indexOf(':') < 0
                            if (isIPv4) {
                                return sAddr
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger("IPAddress", e.localizedMessage + "")
            }
            return "localhost"
        }

        const val CAST_SERVER_PORT = 8080
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
    }
}