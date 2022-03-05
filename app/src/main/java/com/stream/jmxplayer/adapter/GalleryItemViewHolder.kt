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
    //lateinit var playListImageView: ImageView

    fun initHistory(type: Int) {
        if (type == GRID_NO_DELETE || type == SINGLE_NO_DELETE) return
        deleteButton = itemView.findViewById(R.id.history_delete)
        if (type == M3U_LIST) {
            deleteButton.setImageResource(R.drawable.ic_outline_more_vert_24)
        }
//        if (type == SINGLE_NO_DELETE) {
//            deleteButton.visibility = View.GONE
//        }
        //playListImageView = itemView.findViewById(R.id.image_view_m3u)
    }

    companion object {
        const val GRID_NO_DELETE = 0 //normal grid view
        const val SINGLE_DELETE = 1 //history list with delete
        const val SINGLE_NO_DELETE = 2 //playlist in exoplayer
        const val M3U_LIST = 3 //m3u list
    }
}
