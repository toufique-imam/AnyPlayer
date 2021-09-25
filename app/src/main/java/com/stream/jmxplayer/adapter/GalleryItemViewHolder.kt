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
    lateinit var playButton: ImageButton

    fun initHistory() {
        deleteButton = itemView.findViewById(R.id.history_delete)
        playButton = itemView.findViewById(R.id.history_play)
    }

    fun hideButton() {
        deleteButton.visibility = View.GONE
        playButton.visibility = View.GONE
    }
}
