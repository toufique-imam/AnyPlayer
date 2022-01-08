package com.stream.jmxplayer.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.javiersantos.piracychecker.PiracyChecker
import com.google.android.gms.ads.MobileAds
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.IAdListener
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.*
import com.stream.jmxplayer.utils.GlobalFunctions.logger

import com.stream.jmxplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.stream.jmxplayer.utils.ijkplayer.Settings

class SplashActivity : AppCompatActivity() {
    private lateinit var intentNow: Intent
    var adMobAdUtils: AdMobAdUtils? = null
    lateinit var iAdListener: IAdListener
    private lateinit var playerModel: PlayerModel
    private lateinit var alertDialogLoading: AlertDialog


    private lateinit var mSettings: Settings
    var piracyChecker: PiracyChecker? = null
    override fun onDestroy() {
        super.onDestroy()
        piracyChecker?.destroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSettings = Settings(this)
        setTheme(mSettings.themeId)
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()

        logger("Splash", "came here")

        intentNow = intent
        playerModel = PlayerUtils.parseIntent(intentNow)

        alertDialogLoading = this.createAlertDialogueLoading()

        piracyChecker = initPiracy {
            MobileAds.initialize(this) {
                adMobAdUtils = AdMobAdUtils(this)
                adActivity()
            }
        }

    }


    private fun workAfterAdActivity() {
        if (playerModel.link.isEmpty() || playerModel.link == "null") {
            val intent = Intent(this, BrowserActivity::class.java)
            startActivity(intent)
        } else {
            playerModel.id = PlayerModel.getId(playerModel.link, playerModel.title)
            PlayListAll.clear()
            PlayListAll.add(playerModel)
            if (playerModel.link.endsWith("m3u")) {
                playerModel.streamType = PlayerModel.STREAM_M3U
                val intent = Intent(this, BrowserActivity::class.java)
                intent.putExtra(PlayerUtils.M3U_INTENT, true)
                startActivity(intent)
                return
            }
            val intentNext = GlobalFunctions.getDefaultPlayer(this, mSettings, playerModel)
            intentNext.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intentNext.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intentNext.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

            override fun onAdLoaded(type: Int) {
                alertDialogLoading.dismiss()
                adMobAdUtils?.showFullScreenAd()
            }

            override fun onAdError(error: String) {
                alertDialogLoading.dismiss()
                logger("Splash Ad", error)
                workAfterAdActivity()
            }
        }

        if (adMobAdUtils != null) {
            adMobAdUtils!!.setAdListener(iAdListener)
            adMobAdUtils!!.loadFullScreenAd()
        } else {
            workAfterAdActivity()
        }
    }
}