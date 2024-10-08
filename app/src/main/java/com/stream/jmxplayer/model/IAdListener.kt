package com.stream.jmxplayer.model

import androidx.annotation.Keep

@Keep
interface IAdListener {
    fun onAdActivityDone(result: String)
    fun onAdLoadingStarted()
    fun onAdLoaded(type: Int)
    fun onAdError(error: String)
}