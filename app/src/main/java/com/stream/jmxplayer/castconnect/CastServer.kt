package com.stream.jmxplayer.castconnect

import android.content.Context
import android.net.Uri
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.MediaFileUtils
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

const val CAST_SERVER_PORT = 5050

class CastServer(private val context: Context) : NanoHTTPD(CAST_SERVER_PORT) {
    companion object {
        const val TAG = "CastServer"
        private const val MIME_TYPE_IMAGE = "image/jpg"
        private const val MIME_TYPE_AUDIO = "audio/mp3"
        private const val MIME_TYPE_TEXT = "text/plain"
        const val MIME_TYPE_VIDEO = "video/mp4"

        const val PART_ALBUM_ART = "albumart"
        const val PART_SONG = "song"
        const val PARAM_ID = "id"
    }

    private var videoNow: PlayerModel? = null

    override fun serve(
        uri: String?,
        method: Method?,
        headers: MutableMap<String, String>?,
        parms: MutableMap<String, String>?,
        files: MutableMap<String, String>?
    ): Response {
        logger("serve", "$uri $parms")
        //toaster(context, "$uri $parms")
        val id = parms?.get(PARAM_ID).toString()
        this.videoNow = MediaFileUtils.getMovieUri(context, id.toLong())
        logger("serve", this.videoNow.toString())
        //toaster(context, this.videoNow.toString())
        val realPath = MediaFileUtils.getRealPathFromURI(context, Uri.parse(this.videoNow!!.link))
//        toaster(
//            context,
//            realPath
//        )
        videoNow ?: return errorResponse()
        if (videoNow != null) {
            val fis: FileInputStream?
            val video = File(realPath)
            try {
                fis = FileInputStream(video)
            } catch (e: Exception) {
                logger(TAG, e.localizedMessage)
                return errorResponse(e.localizedMessage)
            }
            val st = Response.Status.OK
            return newFixedLengthResponse(st, MIME_TYPE_VIDEO, fis, video.length())
        } else {
            return errorResponse()
        }
    }

    private fun errorResponse(message: String? = "Error"): Response {
        logger("error", message + "")
        return newFixedLengthResponse("JMX PLAYER $message")
    }
}