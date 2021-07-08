package com.stream.jmxplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.stream.jmxplayer.model.IAdListener
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.PlayerUtils
import com.stream.jmxplayer.utils.UnityAdUtils
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds

class SplashActivity : AppCompatActivity() {
    private lateinit var intentNow: Intent
    lateinit var unityAdUtils: UnityAdUtils
    lateinit var iAdListener: IAdListener
    private lateinit var playerModel: PlayerModel
    private lateinit var alertDialogLoading: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        intentNow = intent
        playerModel = PlayerUtils.parseIntent(intentNow)
        alertDialogLoading = GlobalFunctions.createAlertDialogueLoading(this)
        initUnityAd(this)
        //if (moPubUtils != null)
        //    moPubUtils.setAdEventListener(adEventListener);
//        Handler(Looper.myLooper()!!).postDelayed({
//            if (!this.isDestroyed && !this.isFinishing && !UnityAds.isInitialized()) {
//                alertDialogLoading.dismiss()
//                workAfterAdActivity()
//            }
//        }, 5000)
    }

    private fun initUnityAd(activity: Activity) {
        unityAdUtils = UnityAdUtils(this, object :
            IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                adActivity(activity)
            }

            override fun onInitializationFailed(
                p0: UnityAds.UnityAdsInitializationError?,
                p1: String?
            ) {
                logger("unity init error", p0.toString() + " " + p1)
                workAfterAdActivity()
            }
        })
    }

    private fun workAfterAdActivity() {
        val intentNext = Intent(this, PlayerActivity::class.java)
        intentNext.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intentNext.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intentNext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intentNext.putExtra(PlayerModel.DIRECT_PUT, playerModel)
        startActivity(intentNext)
        finish()

    }

    private fun adActivity(activity: Activity) {
        iAdListener = object : IAdListener {
            override fun onAdActivityDone(result: String) {
                alertDialogLoading.dismiss()
                logger("Splash Ad", result)
                workAfterAdActivity()
            }

            override fun onAdLoadingStarted() {
                alertDialogLoading.show()
            }

            override fun onAdLoaded() {
                alertDialogLoading.dismiss()
                unityAdUtils.showAd(activity)
            }

            override fun onAdError(error: String) {
                alertDialogLoading.dismiss()
                logger("Splash Ad", error)
                workAfterAdActivity()
            }
        }
        unityAdUtils.addListener(iAdListener)
        unityAdUtils.loadAd()
    }


}