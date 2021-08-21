package com.stream.jmxplayer.adapter

import android.view.View
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

}