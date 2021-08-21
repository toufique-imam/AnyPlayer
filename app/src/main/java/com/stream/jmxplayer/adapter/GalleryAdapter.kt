package com.stream.jmxplayer.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger

class GalleryAdapter(val onClick: (PlayerModel) -> Unit) :
    RecyclerView.Adapter<GalleryItemViewHolder>(), Filterable {
    private var galleryData = ArrayList<PlayerModel>()
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryItemViewHolder {
        val viewNow =
            LayoutInflater.from(parent.context).inflate(R.layout.gallery_item, null, false)
        return GalleryItemViewHolder(viewNow)
    }

    private fun milliSecondToString(duration: Int): String {
        val seconds = (duration / (1000)) % 60
        val minutes = (duration / (60 * 1000)) % 60
        val hours = (duration / (1000 * 60 * 60)) % 24
        val ans = StringBuilder()
        when {
            hours > 9 -> {
                ans.append(hours);
            }
            hours > 0 -> {
                ans.append("0")
                ans.append(hours)
            }
            else -> {
                ans.append("00");
            }
        }
        ans.append(":")

        when {
            minutes > 9 -> {
                ans.append(minutes);
            }
            minutes > 0 -> {
                ans.append("0")
                ans.append(minutes)
            }
            else -> {
                ans.append("00");
            }
        }
        ans.append(":")
        when {
            seconds > 9 -> {
                ans.append(seconds);
            }
            seconds > 0 -> {
                ans.append("0")
                ans.append(seconds)
            }
            else -> {
                ans.append("00");
            }
        }
        return ans.toString()
    }

    override fun onBindViewHolder(holder: GalleryItemViewHolder, position: Int) {
        val playerModel = galleryData[position]
        Glide.with(holder.imageView)
            .load(Uri.parse(playerModel.link))
            .thumbnail(0.33f)
            .centerCrop()
            .into(holder.imageView)
        holder.durationView.text = milliSecondToString(playerModel.duration)
        holder.titleView.text = playerModel.title
        holder.imageView.setOnClickListener { onClick(playerModel) }
    }

    override fun getItemCount(): Int {
        return galleryData.size
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
                    logger("Adapter", e.localizedMessage + "")
                }
            }

        }
    }
}