package com.stream.jmxplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stream.jmxplayer.databinding.WebVideoItemBinding
import com.stream.jmxplayer.model.PlayerModel

class WebVideoAdapter(val onClick: (PlayerModel, Int) -> Unit, var selectedModel: PlayerModel?) :
    RecyclerView.Adapter<WebVideoAdapter.WebVideoViewHolder>() {
    private val webVideos = ArrayList<PlayerModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebVideoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = WebVideoItemBinding.inflate(inflater, parent, false)
        return WebVideoViewHolder(binding)
    }

    fun updateData(data: List<PlayerModel>) {
        val size = webVideos.size
        webVideos.clear()
        notifyItemRangeRemoved(0, size)
        webVideos.addAll(data)
        notifyItemRangeInserted(0, data.size)
    }

    override fun getItemId(position: Int): Long {
        return webVideos[position].id
    }

    override fun onBindViewHolder(holder: WebVideoViewHolder, position: Int) {
        holder.bind(webVideos[position], webVideos[position] == selectedModel)
    }

    override fun getItemCount(): Int = webVideos.size

    inner class WebVideoViewHolder(private val binding: WebVideoItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                selectedModel = webVideos[layoutPosition]
                onClick(webVideos[layoutPosition], layoutPosition)
            }
        }

        fun bind(playerModel: PlayerModel, selected: Boolean) {
            binding.model = playerModel
            binding.selected = selected
            binding.executePendingBindings()
        }
    }

}