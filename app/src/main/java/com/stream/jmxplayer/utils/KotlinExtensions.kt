package com.stream.jmxplayer.utils

import android.content.res.Resources
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.stream.jmxplayer.model.TrackInfo
import com.stream.jmxplayer.ui.view.IjkVideoView
import tv.danmaku.ijk.media.player.misc.ITrackInfo
import java.util.*
import kotlin.collections.ArrayList

fun View.isVisible() = visibility == View.VISIBLE
fun View.isInvisible() = visibility == View.INVISIBLE
fun View.isGone() = visibility == View.GONE

fun View?.setVisibility(visibility: Int) {
    this?.visibility = visibility
}

fun View?.setVisible() = setVisibility(View.VISIBLE)
fun View?.setInvisible() = setVisibility(View.INVISIBLE)
fun View?.setGone() = setVisibility(View.GONE)

fun ITrackInfo.getName() = String.format(Locale.US, "# %s: %s", language, infoInline)
fun ITrackInfo.getName(idx: Int) =
    String.format(Locale.US, "# %s %d: %s", language, idx, infoInline)

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun LifecycleOwner.isStarted() = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

fun IjkVideoView.getTypeTracksCount(type: Int): Int {
    val trackInfo = trackInfo ?: return 0
    var ans = 0
    for (i in trackInfo) {
        if (i.trackType == type) ans++
    }
    return ans
}

fun IjkVideoView.getTypeTracks(type: Int): ArrayList<TrackInfo> {
    val ans = ArrayList<TrackInfo>()
    val trackInfo = trackInfo ?: return ans
    for ((idx, i) in trackInfo.withIndex()) {
        if (i.trackType == type) {
            ans.add(TrackInfo(idx, i))
        }
    }
    return ans
}

fun IjkVideoView.getVideoTracksCount(): Int {
    return getTypeTracksCount(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO)
}

fun IjkVideoView.getAudioTracksCount(): Int {
    return getTypeTracksCount(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
}

fun IjkVideoView.getSubTracksCount(): Int {
    return getTypeTracksCount(ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE)
}

fun IjkVideoView.getTimeTracksCount(): Int {
    return getTypeTracksCount(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
}

fun IjkVideoView.getVideoTracks(): ArrayList<TrackInfo> {
    return getTypeTracks(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO)
}

fun IjkVideoView.getAudioTracks(): ArrayList<TrackInfo> {
    return getTypeTracks(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
}

fun IjkVideoView.getSubTracks(): ArrayList<TrackInfo> {
    return getTypeTracks(ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE)
}

fun IjkVideoView.getTimeTracks(): ArrayList<TrackInfo> {
    return getTypeTracks(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
}