package com.stream.jmxplayer.ui.fragment.players

import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.C
import com.stream.jmxplayer.model.MediaPlayerControl
import com.stream.jmxplayer.model.PlayerModel

abstract class BasePlayerFragment : Fragment() , MediaPlayerControl {
    abstract fun setPlayerModel(playerModel: PlayerModel)
    abstract fun releasePlayer()
    abstract fun forwardButtonClicked()
    abstract fun playButtonClicked()
    abstract fun pauseButtonClicked()
    abstract fun timerTapped()
    abstract fun mediaOptionButtonClicked()
    abstract fun hideSystemUI()
    abstract fun getMediaOptionButtonVisibility(): Boolean
    abstract fun showMediaInfo()
    abstract fun initPlayer(fromError : Boolean)
    abstract fun changeResize(resize : Int)
    abstract fun clearResumePosition()
    abstract fun updateResumePosition()
}