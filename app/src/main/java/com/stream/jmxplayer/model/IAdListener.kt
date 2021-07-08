package com.stream.jmxplayer.model

interface IAdListener {
    fun onAdActivityDone(result: String)
    fun onAdLoadingStarted()
    fun onAdLoaded()
    fun onAdError(error: String)
}