package com.stream.jmxplayer.castconnect

import android.content.Context
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.stream.jmxplayer.R
import com.stream.jmxplayer.ui.ExpandedControlsActivity

class CastOptionsProvider : OptionsProvider {
    private val actions = ArrayList<String>()
    private val intActions = intArrayOf(0, 1)
    override fun getCastOptions(context: Context): CastOptions {
        actions.clear()
        actions.add(MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK)
        actions.add(MediaIntentReceiver.ACTION_STOP_CASTING)
        val className = ExpandedControlsActivity::class.java.name
        val notificationOptions = NotificationOptions.Builder()
            .setActions(actions, intActions)
            .setTargetActivityClassName(className)
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(className)
            .build()
        val launchOptions = LaunchOptions.Builder()
            .setAndroidReceiverCompatible(true)
            .setRelaunchIfRunning(true)
            .build()
        return CastOptions.Builder()
            .setLaunchOptions(launchOptions)
            .setReceiverApplicationId(context.getString(R.string.receiver_id))
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(p0: Context): MutableList<SessionProvider>? {
        return null
    }
}