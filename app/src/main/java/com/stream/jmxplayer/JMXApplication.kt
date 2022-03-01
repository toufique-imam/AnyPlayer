package com.stream.jmxplayer

import android.app.Application
import androidx.annotation.Keep
import com.stream.jmxplayer.castconnect.CastServer
import com.stream.jmxplayer.utils.GlobalFunctions
import java.io.IOException

@Keep
class JMXApplication : Application() {
    var castServer: CastServer? = null
    override fun onCreate() {
        super.onCreate()
        startCastServer()
    }


    private fun startCastServer() {
        if (castServer == null)
            castServer = CastServer(this)
        try {
            castServer?.start()
        } catch (e: IOException) {
            GlobalFunctions.logger("CAST_SERVER", e.localizedMessage)
        }
    }

    private fun stopCastServer() {
        castServer?.stop()
    }

}