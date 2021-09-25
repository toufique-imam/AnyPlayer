package com.stream.jmxplayer.castconnect

import android.content.Context
import android.net.Uri
import androidx.core.text.isDigitsOnly
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.MediaFileUtils
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class CastServer(private val context: Context) : NanoHTTPD(CAST_SERVER_PORT) {
    companion object {
        const val CAST_SERVER_PORT = 5050
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

        var range: String? = null
        if (headers != null) {
            val rangeKey = "range"
            println("header")
            for (key in headers.keys) {
                if (rangeKey == key) {
                    range = headers[key]
                }
                logger(key, headers[key])
            }
        }
        logger("range", "" + range)
        val response = if (range == null) {
            getFullResponse(parms)
        } else {
            getPartialResponse(parms, range)
        }
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

    private fun getFullResponse(
        parms: MutableMap<String, String>?
    ): Response {
        val id = parms?.get(PARAM_ID).toString()
        this.videoNow = MediaFileUtils.getMovieUri(context, id.toLong())
        val realPath = MediaFileUtils.getRealPathFromURI(context, Uri.parse(this.videoNow!!.link))

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
            return newFixedLengthResponse(
                st, MIME_TYPE_VIDEO,
                fis, video.length()
            )
        } else {
            return errorResponse()
        }
    }

    private fun getPartialResponse(
        parms: MutableMap<String, String>?,
        rangeHeader: String
    ): Response {
        val id = parms?.get(PARAM_ID).toString()
        this.videoNow = MediaFileUtils.getMovieUri(context, id.toLong())
        val realPath = MediaFileUtils.getRealPathFromURI(context, Uri.parse(this.videoNow!!.link))

        videoNow ?: return errorResponse()
        if (videoNow != null) {
            val fis: FileInputStream?
            val video = File(realPath)
            logger("rangeHeader", rangeHeader)
            val rangeValue = rangeHeader.trim().substring("bytes=".length)
            val fileLength = video.length()
            var start: Long
            var end : Long
            logger("rangeValue", "$rangeValue :: $fileLength")

            if (rangeValue.startsWith("-")) {
                end = fileLength - 1
                val temp: Long = rangeValue.substring("-".length).toLong()
                start = fileLength - 1 - temp
            } else {
                val range = rangeValue.split("-")
                for (i in range) {
                    logger("ranges", i)
                }
                start = range[0].toLong()
                end = if (range.size > 1 && range[1].isNotEmpty() && range[1].isDigitsOnly()) {
                    range[1].toLong()
                } else {
                    fileLength - 1
                }
            }
            if (end > fileLength - 1) {
                end = fileLength - 1
            }
            return if (start <= end) {
                val contentLen = end - start + 1
                fis = FileInputStream(video)
                fis.skip(start)
                val response = newChunkedResponse(
                    Response.Status.PARTIAL_CONTENT,
                    MIME_TYPE_VIDEO, fis
                )
                response.addHeader("Content-Length", contentLen.toString())
                response.addHeader("Content-Range", "bytes $start-$end/$fileLength")
                response.addHeader("Content-Type", MIME_TYPE_VIDEO)
                response
            } else {
                errorResponse("RangeNotSatisfied")
            }
        } else {
            return errorResponse()
        }
    }

    private fun errorResponse(message: String? = "Error"): Response {
        logger("error", message + "")
        return newFixedLengthResponse("JMX PLAYER $message")
    }
}