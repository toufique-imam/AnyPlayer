package com.stream.jmxplayer.model

import android.view.View
import android.widget.MediaController

interface IMediaController {

    fun isEnabled(): Boolean
    fun setEnabled(value: Boolean)
    fun hide()
    fun isShowing(): Boolean
    fun setAnchorView(view: View)
    fun setMediaPlayer(player: MediaController.MediaPlayerControl?)
    fun show(timeout: Int)
    fun show()
    fun showOnce(view: View)
}