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

/*
    private fun isAudioRenderer(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        renderedIndex: Int
    ): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(renderedIndex)
        if (trackGroupArray.length == 0) return false

        val trackType = mappedTrackInfo.getRendererType(renderedIndex)
        return C.TRACK_TYPE_AUDIO == trackType
    }
 */
//fun isTypeRender(
//    mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
//    renderedIndex: Int,
//    type: Int
//): Boolean {
//    val trackGroupArray = mappedTrackInfo.getTrackGroups(renderedIndex)
//    if (trackGroupArray.length == 0) return false
//
//    val trackType = mappedTrackInfo.getRendererType(renderedIndex)
//    return type == trackType
//}

//fun DefaultTrackSelector.getTypeTracksCount(type: Int): Int {
//    if (currentMappedTrackInfo == null) return 0
//    var ans = 0
//    for (i in 0 until currentMappedTrackInfo!!.rendererCount) {
//        if (isTypeRender(currentMappedTrackInfo!!, i, type)) {
//            ans += currentMappedTrackInfo!!.getTrackGroups(i).length
//        }
//    }
//    return ans
//}
//
//fun DefaultTrackSelector.getAudioTracksCount(): Int {
//    return getTypeTracksCount(C.TRACK_TYPE_AUDIO)
//}
//
//fun DefaultTrackSelector.getVideoTracksCount(): Int {
//    return getTypeTracksCount(C.TRACK_TYPE_VIDEO)
//}
//
//fun DefaultTrackSelector.getSubtitleTracksCount(): Int {
//    return getTypeTracksCount(C.TRACK_TYPE_TEXT)
//}

//fun DefaultTrackSelector.getTypeTracks(type: Int): ArrayList<TrackInfo> {
//    val ans = ArrayList<TrackInfo>()
//    if (currentMappedTrackInfo == null) return ans
//
//    for (i in 0 until currentMappedTrackInfo!!.rendererCount) {
//        if (isTypeRender(currentMappedTrackInfo!!, i, type)) {
//            val now = currentMappedTrackInfo!!.getTrackGroups(i)
//            for (j in 0 until now.length) {
//                ans.add(TrackInfo(j, i, type, now[j]))
//            }
//        }
//    }
//    return ans
//}

//fun DefaultTrackSelector.getAudioTracks(): ArrayList<TrackInfo> {
//    return getTypeTracks(C.TRACK_TYPE_AUDIO)
//}
//
//fun DefaultTrackSelector.getVideoTracks(): ArrayList<TrackInfo> {
//    return getTypeTracks(C.TRACK_TYPE_VIDEO)
//}
//
//fun DefaultTrackSelector.getSubtitleTracks(): ArrayList<TrackInfo> {
//    return getTypeTracks(C.TRACK_TYPE_TEXT)
//}

//fun TrackSelectionArray.getSelectedTrack(type: Int): TrackGroup? {
//    if (all == null) return null
//    for (i in all) {
//        if (i == null) continue
//        if (i.type != null && i.type == type) {
//            return i.trackGroup
//        }
//    }
//    return null
//}
//
//fun DefaultTrackSelector.getSelectedTrackType(
//    type: Int,
//    trackSelectionArray: TrackSelectionArray
//): Int {
//    val selected = trackSelectionArray.getSelectedTrack(type)
//    if (selected == null) return C.INDEX_UNSET
//    else {
//        if (currentMappedTrackInfo == null) return C.INDEX_UNSET
//
//        for (i in 0 until currentMappedTrackInfo!!.rendererCount) {
//            if (isTypeRender(currentMappedTrackInfo!!, i, type)) {
//                val now = currentMappedTrackInfo!!.getTrackGroups(i)
//                for (j in 0 until now.length) {
//                    if (now[j] == selected) {
//                        return j
//                    }
//                }
//            }
//        }
//        return C.INDEX_UNSET
//    }
//}

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