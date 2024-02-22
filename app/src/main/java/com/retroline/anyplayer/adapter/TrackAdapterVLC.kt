package com.retroline.anyplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.retroline.anyplayer.databinding.VideoTrackItemVlcBinding
import org.videolan.libvlc.MediaPlayer

class TrackAdapterVLC(
    private val tracks: Array<MediaPlayer.TrackDescription>,
    var selectedTrack: MediaPlayer.TrackDescription?
) : RecyclerView.Adapter<TrackAdapterVLC.ViewHolderVLC>() {

    lateinit var trackSelectedListener: (MediaPlayer.TrackDescription) -> Unit


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderVLC {
        val inflater = LayoutInflater.from(parent.context)
        val binding = VideoTrackItemVlcBinding.inflate(inflater, parent, false)
        return ViewHolderVLC(binding)
    }

    fun setOnTrackSelectedListener(listener: (MediaPlayer.TrackDescription) -> Unit) {
        trackSelectedListener = listener
    }

    override fun onBindViewHolder(holder: ViewHolderVLC, position: Int) {
        holder.bind(tracks[position], tracks[position] == selectedTrack)
    }

    override fun getItemCount(): Int = tracks.size


    inner class ViewHolderVLC(private val binding: VideoTrackItemVlcBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {

            itemView.setOnClickListener {
                selectedTrack = tracks[layoutPosition]
                notifyDataSetChanged()
                trackSelectedListener.invoke(tracks[layoutPosition])
            }
        }

        fun bind(trackDescription: MediaPlayer.TrackDescription, selected: Boolean) {
            binding.track = trackDescription
            binding.selected = selected
            binding.executePendingBindings()
        }
    }
}