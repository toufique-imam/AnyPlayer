package com.stream.jmxplayer.model

import androidx.mediarouter.app.MediaRouteButton
import java.io.Serializable

interface ICastController : Serializable {
    fun castPlayerModel(playerModel: PlayerModel)
    fun updateCastButton(mediaRouteButton: MediaRouteButton)
}