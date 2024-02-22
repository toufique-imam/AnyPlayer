package com.retroline.anyplayer.utils

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.retroline.anyplayer.model.PlayerModel
import com.retroline.anyplayer.utils.GlobalFunctions.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class MediaFileUtils {
    companion object {

        private const val TAG = "MediaFileUtils"
        private fun getAlbumArtUri(albumId: Long): Uri {
            return ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            )
        }

        fun getRealPathFromURI(context: Context, contentUri: Uri, type: Int): String {
            val projection =
                when (type) {
                    PlayerModel.STREAM_OFFLINE_VIDEO -> arrayOf(MediaStore.Video.Media.DATA)
                    PlayerModel.STREAM_OFFLINE_AUDIO -> arrayOf(MediaStore.Audio.Media.DATA)
                    else -> arrayOf(MediaStore.Images.Media.DATA)
                }
            //toaster(context, "Querying $contentUri")
            return context.contentResolver.query(contentUri, projection, null, null, null)?.use {
                val dataIndex = when (type) {
                    PlayerModel.STREAM_OFFLINE_VIDEO -> it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    PlayerModel.STREAM_OFFLINE_AUDIO -> it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    else -> it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                }
                if (it.moveToFirst()) {
                    it.getString(dataIndex)
                } else {
                    ""
                }
            } ?: throw IllegalStateException("Unable to query $contentUri, system returned null.")
        }

        private fun makePlayerModel(
            id: Long,
            displayName: String,
            size: Long,
            duration: Int,
            type: Int
        ): PlayerModel {
            val contentUri = getContentUri(type, id)
            val video = PlayerModel(
                title = displayName, id = id,
                link = contentUri.toString(), duration = duration,
                streamType = type
            )
            video.image =
                if (type == PlayerModel.STREAM_OFFLINE_AUDIO) getAlbumArtUri(size).toString() else video.link
            video.cardImageUrl = video.image
            return video
        }

        suspend fun getAllMediaData(type: Int, context: Context): MutableList<PlayerModel> {
            val videoModels = mutableListOf<PlayerModel>()
            withContext(Dispatchers.IO) {
                val collection = getCollection(type)
                val projection = getProjection(type)
                val selection = getSelection(type)

                val selectionArgs = arrayOf(
                    // Release day of the G1. :)
                    dateToTimestamp(day = 22, month = 10, year = 2008).toString()
                )
                val sortOrder = getSortOrder(type)
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->

                    val dbIds = getColumnIndex(type, cursor)

                    logger(TAG, "Found ${cursor.count} videos")
                    while (cursor.moveToNext()) {

                        val id = cursor.getLong(dbIds[0])
                        val displayName = cursor.getString(dbIds[1]) ?: "no name"
//                        val dateModified =
//                            Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dbIds[2])))
                        val size = cursor.getLong(dbIds[3])
                        val duration =
                            if (type != PlayerModel.STREAM_OFFLINE_IMAGE) cursor.getInt(dbIds[4]) else 0
                        videoModels += makePlayerModel(id, displayName, size, duration, type)
                    }
                    cursor.close()
                }
            }
            logger(TAG, "Found ${videoModels.size} videos")
            return videoModels
        }

        @Suppress("SameParameterValue")
        @SuppressLint("SimpleDateFormat")
        private fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
            SimpleDateFormat("dd.MM.yyyy").let { formatter ->
                TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
            }

        fun getMediaUri(context: Context, id: Long, type: Int): PlayerModel? {
            val projection = getProjection(type)
            val collection = getCollection(type)

            val selection = StringBuilder()
            when (type) {
                PlayerModel.STREAM_OFFLINE_VIDEO -> selection.append(MediaStore.Video.Media._ID)
                PlayerModel.STREAM_OFFLINE_AUDIO -> selection.append(MediaStore.Audio.Media._ID)
                else -> selection.append(MediaStore.Images.Media._ID)
            }
            selection.append(" IN (")
            selection.append(id)
            selection.append(")")
            val cursor = context.contentResolver.query(
                collection,
                projection, selection.toString(), null, null
            ) ?: return null
            val ids = getColumnIndex(type, cursor)
            cursor.moveToFirst()

            return try {
                val idNow = cursor.getLong(ids[0])
                val displayName = cursor.getString(ids[1]) ?: "no name"
//                val dateModified =
//                    Date(TimeUnit.SECONDS.toMillis(cursor.getLong(ids[2])))
                val size = cursor.getLong(ids[3])
                val duration =
                    if (type != PlayerModel.STREAM_OFFLINE_IMAGE) cursor.getInt(ids[4]) else 0

                makePlayerModel(idNow, displayName, size, duration, type)
            } catch (e: Exception) {
                logger(TAG, "" + e.localizedMessage)
                null
            }
        }

        fun getCollection(type: Int): Uri {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                when (type) {
                    PlayerModel.STREAM_OFFLINE_VIDEO ->
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    PlayerModel.STREAM_OFFLINE_AUDIO ->
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    else ->
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                }
            } else {
                when (type) {
                    PlayerModel.STREAM_OFFLINE_VIDEO ->
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    PlayerModel.STREAM_OFFLINE_AUDIO ->
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else ->
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }
        }

        private fun getSelection(type: Int): String {
            return when (type) {
                PlayerModel.STREAM_OFFLINE_VIDEO ->
                    "${MediaStore.Video.Media.DATE_ADDED} >= ?"
                PlayerModel.STREAM_OFFLINE_AUDIO ->
                    "${MediaStore.Audio.Media.DATE_ADDED} >= ?"
                else ->
                    "${MediaStore.Images.Media.DATE_ADDED} >= ?"
            }
        }

        private fun getContentUri(type: Int, id: Long): Uri {
            return when (type) {
                PlayerModel.STREAM_OFFLINE_VIDEO ->
                    ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                PlayerModel.STREAM_OFFLINE_AUDIO ->
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                else ->
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
            }
        }

        private fun getColumnIndex(type: Int, cursor: Cursor): Array<Int> {
            return when (type) {
                PlayerModel.STREAM_OFFLINE_VIDEO ->
                    arrayOf(
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID),
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME),
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED),
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE),
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    )
                PlayerModel.STREAM_OFFLINE_AUDIO ->
                    arrayOf(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID),
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME),
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED),
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID),
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    )
                else ->
                    arrayOf(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID),
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME),
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED),
                        //cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DURATION),
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    )
            }
        }


        private fun getSortOrder(type: Int): String {
            return when (type) {
                PlayerModel.STREAM_OFFLINE_VIDEO ->
                    "${MediaStore.Video.Media.DATE_ADDED} DESC"
                PlayerModel.STREAM_OFFLINE_AUDIO ->
                    "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                else ->
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
            }

        }

        private fun getProjection(type: Int): Array<String> {
            return when (type) {
                PlayerModel.STREAM_OFFLINE_VIDEO -> {
                    arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media.SIZE,
                        MediaStore.Video.Media.DURATION,
                    )
                }
                PlayerModel.STREAM_OFFLINE_AUDIO -> {
                    arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.DATE_ADDED,
                        MediaStore.Audio.Media.ALBUM_ID,
                        MediaStore.Audio.Media.DURATION
                    )
                }
                else -> {
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.SIZE
                    )
                }
            }
        }

    }
}