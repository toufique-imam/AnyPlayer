package com.stream.jmxplayer.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.cast.framework.CastContext
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.IAdListener
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.AdMobAdUtils
import com.stream.jmxplayer.utils.GlobalFunctions
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.PlayerUtils
import com.stream.jmxplayer.utils.SharedPreferenceUtils
import com.stream.jmxplayer.utils.SharedPreferenceUtils.Companion.PlayListAll

class SplashActivity : AppCompatActivity() {
    private lateinit var intentNow: Intent
    var adMobAdUtils: AdMobAdUtils? = null
    lateinit var iAdListener: IAdListener
    private lateinit var playerModel: PlayerModel
    private lateinit var alertDialogLoading: AlertDialog
    private var mCastContext: CastContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(SharedPreferenceUtils.getTheme(this))
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()
        logger("Splash", "came here")
        mCastContext = CastContext.getSharedInstance(this)

        intentNow = intent
        playerModel = PlayerUtils.parseIntent(intentNow)


        alertDialogLoading = GlobalFunctions.createAlertDialogueLoading(this)
        workAfterAdActivity()
//        MobileAds.initialize(this) {
//            adMobAdUtils = AdMobAdUtils(this)
//            Handler(Looper.myLooper()!!).postDelayed({
//                adActivity()
//            }, 500)
//        }
    }


    private fun workAfterAdActivity() {
        if (playerModel.link.isEmpty() || playerModel.link == "null") {
            val intent = Intent(this, BrowserActivity::class.java)
            startActivity(intent)
        } else {
            playerModel.id = PlayerModel.getId(playerModel.link, playerModel.title)
            logger("Splash PlayerModel", playerModel.toString())
            val intentNext = Intent(this, PlayerActivity::class.java)
            //val intentNext = Intent(this, VlcActivity::class.java)
            intentNext.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intentNext.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intentNext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            PlayListAll.clear()
            PlayListAll.add(playerModel)
            //intentNext.putExtra(PlayerModel.DIRECT_PUT, data)
            startActivity(intentNext)
        }
        finish()
    }


    private fun adActivity() {
        if (adMobAdUtils == null) {
            workAfterAdActivity()
        }
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
                adMobAdUtils?.showAd()
            }

            override fun onAdError(error: String) {
                alertDialogLoading.dismiss()
                logger("Splash Ad", error)
                workAfterAdActivity()
            }
        }

        if (adMobAdUtils != null) {
            adMobAdUtils!!.setAdListener(iAdListener)
            adMobAdUtils!!.loadAd()
        } else {
            workAfterAdActivity()
        }
    }
}