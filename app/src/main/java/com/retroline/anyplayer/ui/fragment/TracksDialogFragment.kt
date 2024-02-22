package com.retroline.anyplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.retroline.anyplayer.R
import com.retroline.anyplayer.adapter.TrackAdapter
import com.retroline.anyplayer.adapter.TrackAdapterVLC
import com.retroline.anyplayer.databinding.PlayerOverlayTracksBinding
import com.retroline.anyplayer.model.TrackInfo
import com.retroline.anyplayer.ui.view.IjkVideoView
import com.retroline.anyplayer.utils.getAudioTracks
import com.retroline.anyplayer.utils.getAudioTracksCount
import com.retroline.anyplayer.utils.getVideoTracks
import com.retroline.anyplayer.utils.getVideoTracksCount
import com.retroline.anyplayer.utils.setGone
import com.retroline.anyplayer.utils.setVisible
import org.videolan.libvlc.MediaPlayer
import tv.danmaku.ijk.media.player.misc.ITrackInfo

class TracksDialogFragment : AnyBottomSDFragment() {
    private lateinit var binding: PlayerOverlayTracksBinding
    lateinit var trackSelectionListener: (TrackInfo) -> Unit
    lateinit var trackSelectionListenerVLC: (Int, TrackType) -> Unit
    lateinit var onBindInitiated: (done: Boolean) -> Unit

    override fun getDefaultState(): Int = BottomSheetBehavior.STATE_EXPANDED

    override fun needToManageOrientation(): Boolean = true

    override fun initialFocusedView(): View = binding.subtitleTracks.emptyView

    fun onVLCPlayerModelChanged(mVlcPlayer: MediaPlayer) {
        mVlcPlayer.let { playbackService ->
            if (playbackService.videoTracksCount <= 2) {
                binding.videoTracks.trackContainer.setGone()
                binding.tracksSeparator3.setGone()
            }
            if (playbackService.audioTracksCount <= 0) {
                binding.audioTracks.trackContainer.setGone()
                binding.tracksSeparator2.setGone()
            }
            if (playbackService.videoTracks != null)
                playbackService.videoTracks.let { trackList ->
                    val trackAdapter = TrackAdapterVLC(
                        trackList as Array<MediaPlayer.TrackDescription>,
                        trackList.firstOrNull {
                            it.id == playbackService.videoTrack
                        })
                    trackAdapter.setOnTrackSelectedListener { track ->
                        trackSelectionListenerVLC.invoke(track.id, TrackType.VIDEO)
                    }
                    binding.videoTracks.trackList.adapter = trackAdapter
                }

            if (playbackService.audioTracks != null)
                playbackService.audioTracks?.let { trackList ->
                    val trackAdapter = TrackAdapterVLC(
                        trackList as Array<MediaPlayer.TrackDescription>,
                        trackList.firstOrNull {
                            it.id == playbackService.audioTrack
                        })
                    trackAdapter.setOnTrackSelectedListener { track ->
                        trackSelectionListenerVLC.invoke(
                            track.id, TrackType.AUDIO
                        )
                    }
                    binding.audioTracks.trackList.adapter = trackAdapter
                }
            if (playbackService.spuTracks != null)
                playbackService.spuTracks?.let { trackList ->
                    val trackAdapter = TrackAdapterVLC(
                        trackList as Array<MediaPlayer.TrackDescription>,
                        trackList.firstOrNull {
                            it.id == playbackService.spuTrack
                        })
                    trackAdapter.setOnTrackSelectedListener { track ->
                        trackSelectionListenerVLC.invoke(
                            track.id, TrackType.SPU
                        )
                    }
                    binding.subtitleTracks.trackList.adapter = trackAdapter
                }
            if (playbackService.spuTracks == null) binding.subtitleTracks.emptyView.setVisible()
        }
    }

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
            binding.subtitleTracks.emptyView.setVisible()
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
        binding.audioTracks.trackTitle.text = resources.getString(R.string.audio)
        binding.videoTracks.trackTitle.text = resources.getString(R.string.video)
        binding.subtitleTracks.trackTitle.text = resources.getString(R.string.subtitles)

        binding.audioTracks.trackList.layoutManager = LinearLayoutManager(requireActivity())
        binding.videoTracks.trackList.layoutManager = LinearLayoutManager(requireActivity())
        binding.subtitleTracks.trackList.layoutManager = LinearLayoutManager(requireActivity())

        //prevent focus
        binding.tracksSeparator3.isEnabled = false
        binding.tracksSeparator2.isEnabled = false

        super.onViewCreated(view, savedInstanceState)
        onBindInitiated(true)
        //PlaybackService.serviceFlow.onEach { onServiceChanged(it) }.launchIn(lifecycleScope)
    }

    enum class TrackType {
        VIDEO, AUDIO, SPU
    }

    enum class VideoTrackOption {
        SUB_DELAY, SUB_PICK, SUB_DOWNLOAD, AUDIO_DELAY
    }
}