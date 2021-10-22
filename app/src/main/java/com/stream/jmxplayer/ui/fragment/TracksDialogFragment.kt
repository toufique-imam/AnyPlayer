package com.stream.jmxplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stream.jmxplayer.adapter.TrackAdapter
import com.stream.jmxplayer.databinding.PlayerOverlayTracksBinding
import com.stream.jmxplayer.model.TrackInfo
import com.stream.jmxplayer.ui.view.IjkVideoView
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import tv.danmaku.ijk.media.player.misc.ITrackInfo

class TracksDialogFragment : JMXBottomSDFragment() {
    private lateinit var binding: PlayerOverlayTracksBinding
    lateinit var trackSelectionListener: (TrackInfo) -> Unit
    lateinit var onBindInitiated: (done: Boolean) -> Unit

    override fun getDefaultState(): Int = BottomSheetBehavior.STATE_EXPANDED

    override fun needToManageOrientation(): Boolean = true

    override fun initialFocusedView(): View = binding.subtitleTracks.emptyView

    fun onIJKPlayerModelChanged(ijkVideoView: IjkVideoView) {
        ijkVideoView.let { playbackService ->
            if (playbackService.getVideoTracksCount() <= 2) {
                binding.videoTracks.trackContainer.setGone()
                binding.tracksSeparator3.setGone()
            }
            if (playbackService.getAudioTracksCount() <= 0) {
                binding.audioTracks.trackContainer.setGone()
                binding.tracksSeparator2.setGone()
            }
            playbackService.getVideoTracks().let { trackList ->
                val trackAdapter = TrackAdapter(
                    trackList,
                    if (playbackService.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO) == -1)
                        null
                    else trackList.firstOrNull { trackInfo ->
                        trackInfo.id == playbackService.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO)
                    }
                )
                trackAdapter.setOnTrackSelectedListener { track ->
                    trackSelectionListener.invoke(track)
                }
                binding.videoTracks.trackList.adapter = trackAdapter
            }
            playbackService.getAudioTracks().let { trackList ->
                val trackAdapter = TrackAdapter(
                    trackList,
                    if (playbackService.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO) == -1)
                        null
                    else trackList.firstOrNull { trackInfo ->
                        trackInfo.id == playbackService.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
                    }
                )
                trackAdapter.setOnTrackSelectedListener { track ->
                    trackSelectionListener.invoke(track)
                }
                binding.audioTracks.trackList.adapter = trackAdapter
            }
            playbackService.getSubTracks().let { trackList ->
                val trackAdapter = TrackAdapter(
                    trackList,
                    if (playbackService.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) == -1)
                        null
                    else trackList.firstOrNull { trackInfo ->
                        trackInfo.id == playbackService.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE)
                    }
                )
                trackAdapter.setOnTrackSelectedListener { track ->
                    trackSelectionListener.invoke(track)
                }
                binding.subtitleTracks.trackList.adapter = trackAdapter
            }
            if (playbackService.getSubTracksCount() <= 0) {
                playbackService.getTimeTracks().let { trackList ->
                    val trackAdapter = TrackAdapter(
                        trackList,
                        if (playbackService.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) == -1)
                            null
                        else trackList.firstOrNull { trackInfo ->
                            trackInfo.id == playbackService.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
                        }
                    )
                    trackAdapter.setOnTrackSelectedListener { track ->
                        trackSelectionListener.invoke(track)
                    }
                    binding.subtitleTracks.trackList.adapter = trackAdapter
                }
            }
            if (playbackService.getSubTracksCount() <= 0 || playbackService.getTimeTracksCount() <= 0) binding.subtitleTracks.emptyView.setVisible()

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PlayerOverlayTracksBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.audioTracks.trackTitle.text = "Audio"
        binding.videoTracks.trackTitle.text = "Video"
        binding.subtitleTracks.trackTitle.text = "Subtitle"

        binding.audioTracks.trackList.layoutManager = LinearLayoutManager(requireActivity())
        binding.videoTracks.trackList.layoutManager = LinearLayoutManager(requireActivity())
        binding.subtitleTracks.trackList.layoutManager = LinearLayoutManager(requireActivity())

        //prevent focus
        binding.tracksSeparator3.isEnabled = false
        binding.tracksSeparator2.isEnabled = false

        super.onViewCreated(view, savedInstanceState)
        logger("TracksDialog", "view created")
        onBindInitiated(true)
        //PlaybackService.serviceFlow.onEach { onServiceChanged(it) }.launchIn(lifecycleScope)
    }
}