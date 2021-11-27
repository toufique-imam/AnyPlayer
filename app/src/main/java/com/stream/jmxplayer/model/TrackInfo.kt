package com.stream.jmxplayer.model

import androidx.annotation.Keep
import tv.danmaku.ijk.media.player.misc.ITrackInfo

@Keep
class TrackInfo(var id: Int, var iTrackInfo: ITrackInfo) {

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(id)
        stringBuilder.append(" ")
        stringBuilder.append(iTrackInfo.trackType)
        stringBuilder.append(" ")
        stringBuilder.append(iTrackInfo.language)
        stringBuilder.append(" ")
        stringBuilder.append(iTrackInfo.infoInline)
        stringBuilder.append(" ")
        stringBuilder.append(iTrackInfo.format)
        return stringBuilder.toString()
    }
}