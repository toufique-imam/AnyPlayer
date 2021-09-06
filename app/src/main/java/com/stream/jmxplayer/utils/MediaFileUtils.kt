package com.stream.jmxplayer.utils

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.toaster
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MediaFileUtils {
    companion object {

        const val TAG = "MediaFileUtils"
        fun getCollection(): Uri {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        }

        fun getRealPathFromURI(context: Context, contentUri: Uri): String {
            val projection = arrayOf(MediaStore.Video.Media.DATA)
            logger("Querying", contentUri.toString())
            //toaster(context, "Querying $contentUri")
            return context.contentResolver.query(contentUri, projection, null, null, null)?.use {
                val dataIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                if (it.moveToFirst()) {
                    it.getString(dataIndex)
                } else {
                    ""
                }
            } ?: throw IllegalStateException("Unable to query $contentUri, system returned null.")
        }

        fun getAllMovieData(context: Context): MutableList<PlayerModel> {
            val collection = getCollection()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE
            )
            val selection = "${MediaStore.Video.Media.DATE_ADDED} >= ?"
            val selectionArgs = arrayOf(
                // Release day of the G1. :)
                dateToTimestamp(day = 22, month = 10, year = 2008).toString()
            )
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            val videoModels = mutableListOf<PlayerModel>()
            val cursor = context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            ) ?: return videoModels
            val idColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateModifiedColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media.DATE_ADDED
                )
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media.DISPLAY_NAME
                )
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            logger(TAG, "Found ${cursor.count} videos")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateModified =
                    Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                val displayName = cursor.getString(displayNameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val video = PlayerModel(
                    title = displayName, id = id,
                    link = contentUri.toString(), duration = duration,
                    streamType = 2
                )
                //val video = PlayerModel(id, contentUri, displayName, duration, size, dateModified)
                videoModels += video
                logger(TAG, "Added video: $video")
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

        fun getMovieUri(context: Context, id: Long): PlayerModel? {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE
            )
            val collection = getCollection()

            val selection = StringBuilder()
            selection.append(MediaStore.Video.Media._ID + " IN (")
            selection.append(id)
            selection.append(")")
            val cursor = context.contentResolver.query(
                collection,
                projection, selection.toString(), null, null
            ) ?: return null
            val idColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateModifiedColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media.DATE_ADDED
                )
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(
                    MediaStore.Video.Media.DISPLAY_NAME
                )
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            cursor.moveToFirst()
            try {
                val id = cursor.getLong(idColumn)
                val dateModified =
                    Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                val displayName = cursor.getString(displayNameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val contentUri = ContentUris.withAppendedId(
                    collection,
                    id
                )
                return PlayerModel(
                    title = displayName, id = id,
                    link = contentUri.toString(), duration = duration,
                    streamType = 2
                )
            } catch (e: Exception) {
                logger(TAG, "" + e.localizedMessage)
                return null
            }
        }
    }
}