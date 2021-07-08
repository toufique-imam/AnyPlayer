package com.stream.jmxplayer.utils

import android.app.Activity
import android.content.Context
import com.stream.jmxplayer.model.IAdListener
import com.unity3d.ads.*

class UnityAdUtils(context: Context) {
    companion object {
        const val unityAdId = "4191597"
        const val placementId1 = "Interstitial_Android"
        const val testMode = true
        const val TAG = "UnityAd"
    }

    lateinit var iAdListener: IAdListener
    lateinit var iUnityAdsListener: IUnityAdsListener

    init {
        initListener()
        UnityAds.addListener(iUnityAdsListener)
        if (!UnityAds.isInitialized())
            UnityAds.initialize(context, unityAdId, testMode, true)
    }


    fun addListener(iAdListener: IAdListener) {
        this.iAdListener = iAdListener
    }

    private fun initListener() {
        iUnityAdsListener = object : IUnityAdsListener {
            override fun onUnityAdsReady(p0: String?) {
                GlobalFunctions.logger(
                    "$TAG Ad Loaded",
                    "message $p0"
                )
            }

            override fun onUnityAdsStart(p0: String?) {
                GlobalFunctions.logger(
                    "$TAG Ad Start",
                    "message $p0"
                )
            }

            override fun onUnityAdsFinish(p0: String?, p1: UnityAds.FinishState?) {
                GlobalFunctions.logger(
                    "$TAG Ad Finish",
                    "message $p0 $p1"
                )
            }

            override fun onUnityAdsError(p0: UnityAds.UnityAdsError?, p1: String?) {
                GlobalFunctions.logger(
                    "$TAG Ad Error",
                    "message $p0 $p1"
                )
            }

        }
    }

    fun loadAd() {
        iAdListener.onAdLoadingStarted()
        UnityAds.load(placementId1, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(p0: String?) {
                iAdListener.onAdLoaded()
            }

            override fun onUnityAdsFailedToLoad(
                p0: String?,
                p1: UnityAds.UnityAdsLoadError?,
                p2: String?
            ) {
                iAdListener.onAdError("error $p2")
            }

        })
    }

    fun showAd(activity: Activity) {
        UnityAds.show(activity, placementId1, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(
                p0: String?,
                p1: UnityAds.UnityAdsShowError?,
                p2: String?
            ) {
                iAdListener.onAdError("error $p2")
            }

            override fun onUnityAdsShowStart(p0: String?) {
                GlobalFunctions.logger(
                    "$TAG show started",
                    "message $p0"
                )
            }

            override fun onUnityAdsShowClick(p0: String?) {
                GlobalFunctions.logger(
                    "$TAG clicked",
                    "message $p0"
                )
            }

            override fun onUnityAdsShowComplete(
                p0: String?,
                p1: UnityAds.UnityAdsShowCompletionState?
            ) {
                iAdListener.onAdActivityDone("Success $p0")
            }

        })
    }

}