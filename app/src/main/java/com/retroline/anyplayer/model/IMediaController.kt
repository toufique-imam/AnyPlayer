package com.retroline.anyplayer.model

import android.view.View
import android.widget.MediaController
import androidx.annotation.Keep

@Keep
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