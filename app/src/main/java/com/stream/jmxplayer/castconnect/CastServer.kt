package com.stream.jmxplayer.castconnect

import android.content.Context
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.CAST_SERVER_PORT
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.MediaFileUtils
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

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
        session: IHTTPSession
    ): Response {
        val id = session.parameters[PARAM_ID].toString()
        this.videoNow = MediaFileUtils.getMovieUri(context, id.toLong())
        videoNow ?: return errorResponse()
        if (videoNow != null) {
            val fis: FileInputStream?
            val video = File(videoNow!!.link)
            try {
                fis = FileInputStream(video)
            } catch (e: Exception) {
                logger(TAG, e.localizedMessage + "")
                return errorResponse(e.localizedMessage)
            }
            val st = Response.Status.OK
            return newFixedLengthResponse(st, MIME_TYPE_VIDEO, fis, video.length())
        } else {
            return errorResponse()
        }
    }

    private fun errorResponse(message: String? = "Error"): Response {
        return newFixedLengthResponse(message)
    }
}