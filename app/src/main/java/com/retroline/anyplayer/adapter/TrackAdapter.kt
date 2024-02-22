package com.retroline.anyplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.retroline.anyplayer.databinding.VideoTrackItemBinding
import com.retroline.anyplayer.model.TrackInfo

class TrackAdapter(
    private val tracks: ArrayList<TrackInfo>,
    var selectedTrack: TrackInfo?
) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    lateinit var trackSelectedListener: (TrackInfo) -> Unit


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = VideoTrackItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    fun setOnTrackSelectedListener(listener: (TrackInfo) -> Unit) {
        trackSelectedListener = listener
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tracks[position], tracks[position] == selectedTrack)
    }

    override fun getItemCount(): Int = tracks.size


    inner class ViewHolder(private val binding: VideoTrackItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {

            itemView.setOnClickListener {
                selectedTrack = tracks[layoutPosition]
                notifyDataSetChanged()
                trackSelectedListener.invoke(tracks[layoutPosition])
            }
        }

        fun bind(trackDescription: TrackInfo, selected: Boolean) {
            binding.track = trackDescription
            binding.selected = selected
            binding.executePendingBindings()
        }
    }
}