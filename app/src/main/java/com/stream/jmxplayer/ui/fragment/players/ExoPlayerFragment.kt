package com.stream.jmxplayer.ui.fragment.players

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.material.button.MaterialButton
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.ui.fragment.BrowseFragment
import com.stream.jmxplayer.ui.view.IjkVideoView
import com.stream.jmxplayer.ui.view.TableLayoutBinder
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.logger
import kotlin.math.max

class ExoPlayerFragment : BasePlayerFragment() {
    private var mPlayer: ExoPlayer? = null
    private var currentMediaItemIndex = 0
    private var playbackPosition = 0L
    private lateinit var mPlayerView: StyledPlayerView
    private var trackSelectorDialog = ArrayList<Dialog?>()
    private var trackDialog: AlertDialog? = null
    private val loadControl = DefaultLoadControl()
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var playerModelNow: PlayerModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong(_playbackPosition, 0)
            currentMediaItemIndex = savedInstanceState.getInt(_currentWindowIndex)
            autoPlay = savedInstanceState.getBoolean(_autoPlay)
        }
        return inflater.inflate(R.layout.content_exo, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPlayerView = view.findViewById(R.id.media_view)
        mPlayerView.controllerAutoShow = false
        mPlayerView.useController = false

    }

    override fun start() {
        if ((mPlayer?.mediaItemCount ?: 0) > 0)
            mPlayer?.play()
    }

    override fun pause() {
        mPlayer?.pause()
    }

    override fun getDuration(): Int = (mPlayer?.duration?.toInt()) ?: 0


    override fun getCurrentPosition(): Int = (mPlayer?.currentPosition?.toInt()) ?: 0

    override fun seekTo(pos: Int) {
        logger("seek", "$pos")
        val seek = (pos * 1f) / (duration * 1f)
        mPlayer?.seekTo(seek.toLong())
    }

    override fun isPlaying(): Boolean = mPlayer?.isPlaying == true

    override fun getBufferPercentage(): Int = (mPlayer?.bufferedPercentage) ?: 0

    override fun canPause(): Boolean = true

    override fun canSeekBackward(): Boolean = mPlayer?.isCurrentMediaItemSeekable == true

    override fun canSeekForward(): Boolean = mPlayer?.isCurrentMediaItemSeekable == true

    override fun getAudioSessionId(): Int = mPlayer?.audioSessionId ?: 0

    private fun addSource() {
        trackSelectorDialog.clear()
        trackDialog = null
        val mediaSource =
            PlayerUtils.createMediaSource(requireActivity(), playerModelNow, errorCount)
        mPlayer?.setMediaSource(mediaSource)
    }

    override fun setPlayerModel(playerModel: PlayerModel) {
        this.playerModelNow = playerModel
        initPlayer(false)
        preparePlayer()
    }

    override fun forwardButtonClicked() {
        if (mPlayer != null) {
            mPlayer?.seekTo(mPlayer?.currentPosition!! + 10000)
        }
    }

    override fun playButtonClicked() {
        mPlayer?.play()
    }

    override fun pauseButtonClicked() {
        mPlayer?.pause()
    }

    override fun timerTapped() {
        TODO("Not yet implemented")
    }

    override fun mediaOptionButtonClicked() {
        if (trackDialog == null)
            initTrackSelector()
        trackDialog?.show()
    }

    override fun hideSystemUI() {
        if (!this.isVisible) return
        val window = requireActivity().window
        if (Build.VERSION.SDK_INT < 30) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(
                    WindowInsets.Type.statusBars()
                            or WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        if (Build.VERSION.SDK_INT < 30) {
            mPlayerView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        } else {
            val controller = mPlayerView.windowInsetsController
            if (controller != null) {
                controller.hide(
                    (WindowInsets.Type.statusBars()
                            or WindowInsets.Type.navigationBars()
                            or WindowInsets.Type.systemBars())
                )
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    override fun getMediaOptionButtonVisibility(): Boolean {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        return mappedTrackInfo != null
    }

    private fun trackSelected(
        trackSelected: TrackGroupArray?,
        trackGroupNow: TrackGroup
    ): Boolean {
        if (trackSelected != null) {
            for (i in 0 until trackSelected.length) {
                if (trackSelected[i] == trackGroupNow) return true
            }
        }
        return false
    }


    override fun showMediaInfo() {
        val mediaMetaData = mPlayer?.mediaMetadata ?: return
        val builder = TableLayoutBinder(requireContext())
        builder.appendSection(R.string.mi_player)
        builder.appendRow2(R.string.mi_player, "ExoPlayer")
        builder.appendSection(R.string.mi_media)

        builder.appendRow2(
            R.string.mi_resolution,
            IjkVideoView.buildResolution(
                mPlayer?.videoFormat?.width ?: 0,
                mPlayer?.videoFormat?.height ?: 0,
                0, 0
            )
        )

        builder.appendRow2(
            R.string.mi_length,
            IjkVideoView.buildTimeMilli(mPlayer?.duration ?: 0)
        )
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        var index = -1
        if (mappedTrackInfo != null) {
            for (i in 0 until mappedTrackInfo.rendererCount) {
                val trackGroupArray = mappedTrackInfo.getTrackGroups(i)
                val trackType = mappedTrackInfo.getRendererType(i)
                val typeStr = when (trackType) {
                    C.TRACK_TYPE_VIDEO -> getString(R.string.TrackType_video)
                    C.TRACK_TYPE_AUDIO -> getString(R.string.TrackType_audio)
                    C.TRACK_TYPE_TEXT -> getString(R.string.TrackType_subtitle)
                    C.TRACK_TYPE_METADATA -> getString(R.string.TrackType_metadata)
                    C.TRACK_TYPE_DEFAULT -> "Default"
                    else -> getString(R.string.TrackType_unknown)
                }
                for (j in 0 until trackGroupArray.length) {
                    val y: TrackGroup = trackGroupArray[j]
                    index++
                    if (trackSelected(mPlayer?.currentTrackGroups, y)) {
                        builder.appendSection(
                            getString(R.string.mi_stream_fmt1, index) + " " +
                                    getString(R.string.mi__selected_video_track)
                        )
                    } else {
                        builder.appendSection(
                            getString(
                                R.string.mi_stream_fmt1,
                                index
                            )
                        )
                    }
                    if (y.length > 0) {
                        val format = y.getFormat(0)
                        builder.appendRow2(R.string.mi_type, typeStr)
                        builder.appendRow2(R.string.mi_language, format.language)

                        if (trackType == C.TRACK_TYPE_AUDIO || trackType == C.TRACK_TYPE_VIDEO) {
                            builder.appendRow2(
                                R.string.mi_codec, format.codecs
                            )
                            if (trackType == C.TRACK_TYPE_VIDEO) {
                                builder.appendRow2(
                                    R.string.mi_pixel_format,
                                    format.pixelCount.toString()
                                )
                                builder.appendRow2(
                                    R.string.mi_resolution,
                                    IjkVideoView.buildResolution(format.height, format.width, 0, 0)
                                )
                                builder.appendRow2(
                                    R.string.mi_frame_rate,
                                    format.frameRate.toString()
                                )
                            } else {
                                builder.appendRow2(
                                    R.string.mi_sample_rate,
                                    format.sampleRate.toString()
                                )
                                builder.appendRow2(
                                    R.string.mi_channels,
                                    format.channelCount.toString()
                                )
                            }

                            builder.appendRow2(
                                R.string.mi_bit_rate,
                                format.averageBitrate.toString()
                            )
                        }
                    }
                }
            }
        }
        val adBuilder = builder.buildAlertDialogBuilder()
        adBuilder.setTitle(R.string.media_information)
        adBuilder.setNegativeButton(R.string.close, null)
        adBuilder.show()
    }

    private fun isAudioRenderer(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        renderedIndex: Int
    ): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(renderedIndex)
        if (trackGroupArray.length == 0) return false

        val trackType = mappedTrackInfo.getRendererType(renderedIndex)
        return C.TRACK_TYPE_AUDIO == trackType
    }

    private fun isSubtitleRenderer(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        renderedIndex: Int
    ): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(renderedIndex)
        if (trackGroupArray.length == 0) return false

        val trackType = mappedTrackInfo.getRendererType(renderedIndex)
        return C.TRACK_TYPE_TEXT == trackType
    }

    private fun isVideoRenderer(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        renderedIndex: Int
    ): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(renderedIndex)
        if (trackGroupArray.length == 0) return false

        val trackType = mappedTrackInfo.getRendererType(renderedIndex)
        return C.TRACK_TYPE_VIDEO == trackType
    }

    private fun initTrackSelector() {
        trackSelectorDialog.clear()
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_tracks, null)

        val builder =
            AlertDialog.Builder(requireContext()).setTitle("Select Tracks").setView(dialogView)
        builder.setCancelable(true)
        val radioGroup: RadioGroup = dialogView.findViewById(R.id.radio_group_server)

        trackDialog = builder.create()
        var cnt = 0
        trackSelectorDialog.clear()
        for (i in 0 until mappedTrackInfo.rendererCount) {
            if (isVideoRenderer(mappedTrackInfo, i) || isAudioRenderer(
                    mappedTrackInfo,
                    i
                ) || isSubtitleRenderer(mappedTrackInfo, i)
            ) {
                trackSelectorDialog.add(
                    TrackSelectionDialogBuilder(
                        requireContext(),
                        "Select audio track",
                        mPlayer!!,
                        i
                    ).build()
                )
                val radioButton = RadioButton(requireContext())
                radioButton.id = cnt
                radioButton.text = mappedTrackInfo.getRendererName(i)
                radioButton.textSize = 15f
                radioButton.setTextColor(Color.BLACK)
                if (cnt == 0) {
                    radioButton.isChecked = true
                }
                radioGroup.addView(radioButton)
                cnt++
            }
        }


        val materialButton: MaterialButton = dialogView.findViewById(R.id.button_stream_now)

        materialButton.setOnClickListener {
            trackDialog?.dismiss()
            trackSelectorDialog[radioGroup.checkedRadioButtonId]?.show()
        }
    }

    private fun createPlayer() {
        trackSelector = DefaultTrackSelector(requireContext())
        trackSelector.setParameters(
            trackSelector.parameters.buildUpon()
                .setPreferredTextLanguage("es")
                .setPreferredAudioLanguage("es")
        )
        val extensionRendererMode =
            when (renderExo) {
                1 -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                2 -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                3 -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                else -> {
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                }
            }
        val renderer = DefaultRenderersFactory(requireContext())
            .setExtensionRendererMode(extensionRendererMode)

        mPlayer = ExoPlayer.Builder(requireContext(), renderer)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(15000)
            .setSeekForwardIncrementMs(15000)
            .build()
    }

    private fun preparePlayer() {
        mPlayer?.prepare()
        if (autoPlay) {
            mPlayer?.play()
        } else {
            mPlayer?.pause()
        }
    }

    override fun initPlayer(fromError: Boolean) {
        inErrorState = false
        if (mPlayer == null) {
            createPlayer()
            mPlayer?.playWhenReady = autoPlay
            if (activity is Player.Listener) {
                mPlayer?.addListener(activity as Player.Listener)
            }
            if (activity is AnalyticsListener) {
                mPlayer?.addAnalyticsListener(activity as AnalyticsListener)
            }
            mPlayerView.player = mPlayer
        }
        if (!fromError) {
            addSource()
        }
        preparePlayer()
    }

    override fun changeResize(resize: Int) {
        mPlayerView.resizeMode = resize
    }

    override fun releasePlayer() {
        if (mPlayer != null) {
            trackDialog = null
            trackSelectorDialog.clear()
            // save the player state before releasing its resources
            playbackPosition = mPlayer?.currentPosition ?: 0L
            currentMediaItemIndex = mPlayer?.currentMediaItemIndex ?: 0
            autoPlay = mPlayer?.playWhenReady ?: true
            mPlayer?.release()
            mPlayer = null
        }
    }

    override fun clearResumePosition() {
        currentMediaItemIndex = C.INDEX_UNSET
        playbackPosition = C.TIME_UNSET
    }

    override fun updateResumePosition() {
        currentMediaItemIndex = mPlayer?.currentMediaItemIndex ?: 0
        playbackPosition = max(0, mPlayer?.contentPosition ?: 0)
    }


    companion object {
        private val _autoPlay = "autoplay"
        private val _currentWindowIndex = "current_window_index"
        private val _playbackPosition = "playback_position"

        private var autoPlay = true
        private var inErrorState = false
        private var errorCount = 0
        var renderExo = 1

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment BrowseFragment.
         */
        @JvmStatic
        fun newInstance() = ExoPlayerFragment()
    }
}