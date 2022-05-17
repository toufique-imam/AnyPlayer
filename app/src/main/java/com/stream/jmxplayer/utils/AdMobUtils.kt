package com.stream.jmxplayer.utils

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.IAdListener
import kotlin.math.max

class AdMobUtils(var activity: Activity) {
    var mInterstitialAd: InterstitialAd? = null
    var mRewardedAd: RewardedAd? = null
    private var adRequest: AdRequest = AdRequest.Builder().build()
    lateinit var iAdListener: IAdListener
    private val preference =
        activity.getSharedPreferences(SharedPreferenceUtils.SHARED_PREF, Activity.MODE_PRIVATE)
    private val editor = preference.edit()
    fun setAdListener(iAdListener: IAdListener) {
        this.iAdListener = iAdListener
    }

    fun loadRewardAd() {
        if (GlobalFunctions.isProVersion()) {
            iAdListener.onAdActivityDone("paid")
            return
        }
        iAdListener.onAdLoadingStarted()
        RewardedAd.load(
            activity,
            activity.getString(R.string.REWARD_AD_ID),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(p0: RewardedAd) {
                    mRewardedAd = p0
                    iAdListener.onAdLoaded(1)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    mRewardedAd = null
                    iAdListener.onAdError(p0.message)
                }
            })
    }

    fun loadFullScreenAd() {
        if (GlobalFunctions.isProVersion()) {
            iAdListener.onAdActivityDone("paid")
            return
        }
        if (isRewarded()) {
            iAdListener.onAdActivityDone("reward")
            return
        }
        iAdListener.onAdLoadingStarted()
        InterstitialAd.load(activity,
            activity.getString(R.string.Interstitial_AD_ID),
            adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: InterstitialAd) {
                    mInterstitialAd = p0
                    iAdListener.onAdLoaded(0)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    mInterstitialAd = null
                    iAdListener.onAdError(p0.message)
                }
            })
    }

    fun showFullScreenAd() {
        if (mInterstitialAd == null) {
            iAdListener.onAdError("null")
            return
        }
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
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
        mInterstitialAd?.show(activity)
    }

    fun showRewardAd() {
        if (mRewardedAd == null) {
            iAdListener.onAdError("null")
            return
        }
        mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
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
        mRewardedAd!!.show(activity) {
            saveReward()
        }
    }

    private val STOP_AD = "JMX_REWARD_PRO"
    private fun saveReward() {
        val time: Long
        val timeNow = preference.getLong(STOP_AD, 0L)
        time = max(timeNow, System.currentTimeMillis()) + 3600000L
        editor.putLong(STOP_AD, time)
        editor.apply()
    }

    private fun isRewarded(): Boolean {
        val timeNow = System.currentTimeMillis()
        val timeReward = preference.getLong(STOP_AD, 0L)
        return timeNow < timeReward
    }

}