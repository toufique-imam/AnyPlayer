package com.stream.jmxplayer.model

interface IAdListener {
    fun onAdActivityDone(result: String)
    fun onAdLoadingStarted()
    fun onAdLoaded(type: Int)
    fun onAdError(error: String)
}