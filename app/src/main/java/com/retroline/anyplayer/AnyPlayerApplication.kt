package com.retroline.anyplayer

import android.app.Application
import com.retroline.anyplayer.castconnect.CastServer
import com.retroline.anyplayer.utils.GlobalFunctions
import java.io.IOException


class AnyPlayerApplication : Application() {
    private var castServer: CastServer? = null
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