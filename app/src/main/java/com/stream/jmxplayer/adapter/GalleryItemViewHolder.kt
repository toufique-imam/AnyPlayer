package com.stream.jmxplayer.adapter

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stream.jmxplayer.R

class GalleryItemViewHolder(
    itemView: View,
    val imageView: ImageView = itemView.findViewById(R.id.image_view_item),
    val durationView: TextView = itemView.findViewById(R.id.text_view_duration),
    val titleView: TextView = itemView.findViewById(R.id.text_view_title)

) : RecyclerView.ViewHolder(itemView) {
    lateinit var deleteButton: ImageButton
    lateinit var playListImageView: ImageView

    fun initHistory() {
        deleteButton = itemView.findViewById(R.id.history_delete)
        playListImageView = itemView.findViewById(R.id.image_view_m3u)
    }

    fun configPlaylist() {
        deleteButton.visibility = View.GONE
    }

    fun configM3UPlaylist() {
        imageView.visibility = View.GONE
        playListImageView.visibility = View.VISIBLE
    }

    companion object {
        const val GRID_NO_DELETE = 0
        const val SINGLE_DELETE = 1
        const val SINGLE_NO_DELETE = 2
        const val M3U_LIST = 3
    }
}
