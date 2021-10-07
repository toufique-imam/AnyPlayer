package com.stream.jmxplayer.utils

import android.app.Activity
import android.content.Context
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.PlayerModel

class SharedPreferenceUtils {
    companion object {
        val PlayListAll = ArrayList<PlayerModel>()
        const val SHARED_PREF = "JMX_PLAYER"
        const val THEME = "JMX_THEME"
        fun saveUserLastInput(
            context: Context,
            link: String,
            title: String,
            userAgent: String,
            header: String
        ) {
            val prefs = context.getSharedPreferences(SHARED_PREF, Activity.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(PlayerModel.linkIntent, link)
            editor.putString(PlayerModel.titleIntent, title)
            editor.putString(PlayerModel.userAgentIntent, userAgent)
            editor.putString(PlayerModel.headerIntent, header)
            editor.apply()
        }

        private fun init(int: Int): String {
            return ""
        }

        fun getUserLastInput(context: Context): Array<String> {
            val prefs = context.getSharedPreferences(SHARED_PREF, Activity.MODE_PRIVATE)
            val array = Array(4, this::init)

            array[0] = prefs.getString(PlayerModel.linkIntent, "") ?: ""
            array[1] = prefs.getString(PlayerModel.titleIntent, "") ?: ""
            array[2] = prefs.getString(PlayerModel.userAgentIntent, "") ?: ""
            array[3] = prefs.getString(PlayerModel.headerIntent, "") ?: ""
            return array
        }

        fun getUserM3U(context: Context): Array<String> {
            val prefs = context.getSharedPreferences(SHARED_PREF, Activity.MODE_PRIVATE)
            val array = Array(2, this::init)
            array[0] = prefs.getString(PlayerModel.mainLinkIntent, "") ?: ""
            array[1] = prefs.getString(PlayerModel.descriptionIntent, "") ?: ""
            return array
        }

        fun setUserM3U(context: Context, link: String, title: String) {
            val prefs = context.getSharedPreferences(SHARED_PREF, Activity.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(PlayerModel.mainLinkIntent, link)
            editor.putString(PlayerModel.descriptionIntent, title)

            editor.apply()
        }

        fun getUserTheme(context: Context): String {
            val prefs = context.getSharedPreferences(SHARED_PREF, Activity.MODE_PRIVATE)
            return prefs.getString(THEME, "System") ?: "System"
        }

        fun setUserTheme(context: Context, themeName: String) {
            val prefs = context.getSharedPreferences(SHARED_PREF, Activity.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(THEME, themeName)
            editor.apply()
        }

        fun getTheme(context: Context): Int {
            return when (getUserTheme(context)) {
                "Day" -> R.style.Theme_JMXPlayer_Day
                "Night" -> R.style.Theme_JMXPlayer_Night
                else -> R.style.Theme_JMXPlayer_NoActionBar
            }
        }
    }
}