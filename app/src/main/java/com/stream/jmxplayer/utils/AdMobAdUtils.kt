package com.stream.jmxplayer.utils

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.IAdListener

class AdMobAdUtils(var activity: Activity) {
    var mInterstitialAd: InterstitialAd? = null
    var adRequest: AdRequest = AdRequest.Builder().build()
    lateinit var iAdListener: IAdListener

    fun setAdListener(iAdListener: IAdListener) {
        this.iAdListener = iAdListener
    }

    fun loadAd() {
        iAdListener.onAdLoadingStarted()
        InterstitialAd.load(activity,
            activity.getString(R.string.Interstitial_AD_ID),
            adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: InterstitialAd) {
                    mInterstitialAd = p0
                    iAdListener.onAdLoaded()
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    mInterstitialAd = null
                    iAdListener.onAdError(p0.message)
                }
            })
    }

    fun showAd() {
        if (mInterstitialAd == null) return
        mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                iAdListener.onAdError(p0.message)
            }

            override fun onAdShowedFullScreenContent() {
                mInterstitialAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                iAdListener.onAdActivityDone("Done")
            }
        }
        mInterstitialAd!!.show(activity)
    }

}