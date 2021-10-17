package com.stream.jmxplayer.adapter

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger

class GalleryAdapter(
    val type: Int, val onClick: (PlayerModel, Int) -> Unit,
    val onDelete: (PlayerModel, Int) -> Unit
) :
    RecyclerView.Adapter<GalleryItemViewHolder>(), Filterable {
    var galleryData = ArrayList<PlayerModel>()
    private var mainData = ArrayList<PlayerModel>()

    fun updateData(data: List<PlayerModel>) {
        val size = galleryData.size
        mainData.clear()
        mainData.addAll(data)
        galleryData.clear()
        notifyItemRangeRemoved(0, size)
        galleryData.addAll(data)
        notifyItemRangeInserted(0, data.size)
    }

    fun addData(playerModel: PlayerModel): Int {
        val sz = galleryData.size
        if (mainData.contains(playerModel)) return sz - 1
        mainData.add(playerModel)
        galleryData.add(playerModel)
        notifyItemInserted(sz)
        return sz
    }

    fun deleteData(playerModel: PlayerModel) {
        val position = galleryData.indexOf(playerModel)
        logger("deleteData1", "$playerModel $position")
        logger("deleteData2", "${galleryData[position]} ${mainData[position]}")
        if (position != -1 && position < galleryData.size) {
            galleryData.removeAt(position)
            mainData.remove(playerModel)
            notifyItemRemoved(position)
        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryItemViewHolder {
        val viewNow =
            when (type) {
                GalleryItemViewHolder.GRID_NO_DELETE -> {
                    LayoutInflater.from(parent.context).inflate(R.layout.gallery_item, null, false)
                }
                GalleryItemViewHolder.SINGLE_NO_DELETE -> {
                    LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, null, false)
                }
                else -> {
                    LayoutInflater.from(parent.context).inflate(R.layout.history_item, null, false)
                }
            }

        return GalleryItemViewHolder(viewNow).apply {
//            if (type != GalleryItemViewHolder.GRID_NO_DELETE) {
            initHistory(type)
//            }
//            if (type == GalleryItemViewHolder.SINGLE_NO_DELETE || type == GalleryItemViewHolder.M3U_LIST) {
//                configPlaylist()
//            }
        }
    }

    override fun onBindViewHolder(holder: GalleryItemViewHolder, position: Int) {
        val playerModel = galleryData[position]
        //if (type != GalleryItemViewHolder.M3U_LIST) {
        Glide.with(holder.imageView)
            .load(
                if (playerModel.streamType != PlayerModel.STREAM_M3U) {
                    Uri.parse(playerModel.image)
                } else {
                    R.drawable.logo_playlist2
                }
            )
            .thumbnail(0.33f)
            .placeholder(
                if (playerModel.streamType == PlayerModel.STREAM_OFFLINE_AUDIO) R.drawable.ic_empty_music2
                else R.drawable.main_logo
            )
            //.centerCrop()
            .into(holder.imageView)
        //  }
        if (type != GalleryItemViewHolder.M3U_LIST) {
            val timerText = GlobalFunctions.milliSecondToString(playerModel.duration)
            if (timerText.isEmpty()) holder.durationView.visibility = View.GONE
            else {
                holder.durationView.visibility = View.VISIBLE
                holder.durationView.text = timerText
            }
        } else {
            holder.durationView.text = playerModel.link
        }

        holder.titleView.text = playerModel.title
        if (type == GalleryItemViewHolder.SINGLE_NO_DELETE) {
            holder.titleView.setTextColor(Color.WHITE)
            holder.durationView.setTextColor(Color.WHITE)
        }
        holder.itemView.setOnClickListener { onClick(playerModel, position) }
        if (type == GalleryItemViewHolder.SINGLE_DELETE || type == GalleryItemViewHolder.M3U_LIST) {
            holder.deleteButton.setOnClickListener {
                onDelete(playerModel, position)
            }
        }
    }


    override fun getItemCount(): Int {
        return galleryData.size
    }

    override fun getItemId(position: Int): Long {
        return PlayerModel.getId(galleryData[position].link, galleryData[position].title)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterStr = constraint.toString().lowercase()
                var filtered = ArrayList<PlayerModel>()
                if (filterStr.isEmpty()) {
                    filtered = mainData
                } else {
                    for (i in mainData) {
                        if (i.title.lowercase().contains(filterStr)) {
                            filtered.add(i)
                        }
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = filtered
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                try {
                    val sz = galleryData.size
                    notifyItemRangeRemoved(0, sz)
                    galleryData = if (results != null) {
                        results.values as ArrayList<PlayerModel>
                    } else {
                        ArrayList()
                    }
                    notifyItemRangeInserted(0, galleryData.size)
                } catch (e: Exception) {
                    logger("Adapter", e.localizedMessage)
                }
            }

        }
    }
}