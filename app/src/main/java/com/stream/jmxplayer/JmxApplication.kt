package com.stream.jmxplayer

import android.app.Application
import com.google.android.gms.cast.framework.CastContext

class JmxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val castContext = CastContext.getSharedInstance()

    }
}