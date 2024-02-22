package com.retroline.anyplayer.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.github.javiersantos.piracychecker.PiracyChecker
import com.google.android.gms.ads.MobileAds
import com.retroline.anyplayer.R
import com.retroline.anyplayer.model.IAdListener
import com.retroline.anyplayer.model.PlayerModel
import com.retroline.anyplayer.utils.AdMobUtils
import com.retroline.anyplayer.utils.GlobalFunctions
import com.retroline.anyplayer.utils.GlobalFunctions.logger
import com.retroline.anyplayer.utils.PlayerUtils
import com.retroline.anyplayer.utils.Settings
import com.retroline.anyplayer.utils.SharedPreferenceUtils.Companion.PlayListAll
import com.retroline.anyplayer.utils.initPiracy
import com.retroline.anyplayer.utils.setGone
import com.retroline.anyplayer.utils.setVisible

class SplashActivity : AppCompatActivity() {
    private lateinit var intentNow: Intent
    var adMobUtils: AdMobUtils? = null
    private lateinit var linearLayoutLoading: LinearLayout
    private lateinit var iAdListener: IAdListener
    private lateinit var playerModel: PlayerModel


    private lateinit var mSettings: Settings
    private var piracyChecker: PiracyChecker? = null
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
        linearLayoutLoading = findViewById(R.id.linear_layout_loading)
        linearLayoutLoading.setGone()

        intentNow = intent
        playerModel = PlayerUtils.parseIntent(intentNow)

//        alertDialogLoading = this.createAlertDialogueLoading()

        piracyChecker = initPiracy {
            MobileAds.initialize(this) {
            }
            adMobUtils = AdMobUtils(this)
            adActivity()
        }

    }


    private fun workAfterAdActivity() {
        if (playerModel.link.isEmpty() || playerModel.link == "null") {
            val intent = Intent(this, BrowserActivity::class.java)
            startActivity(intent)
        } else {
            playerModel.id =
                PlayerModel.getId(playerModel.link, playerModel.title, playerModel.streamType)
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
        if (adMobUtils == null) {
            workAfterAdActivity()
        }
        iAdListener = object : IAdListener {
            override fun onAdActivityDone(result: String) {
//                alertDialogLoading.dismiss()
                logger("Splash Ad", result)
                workAfterAdActivity()
            }

            override fun onAdLoadingStarted() {
                linearLayoutLoading.setVisible()
            }

            override fun onAdLoaded(type: Int) {
//                alertDialogLoading.dismiss()
                adMobUtils?.showFullScreenAd()
            }

            override fun onAdError(error: String) {
//                alertDialogLoading.dismiss()
                logger("Splash Ad", error)
                workAfterAdActivity()
            }
        }

        if (adMobUtils != null) {
            adMobUtils!!.setAdListener(iAdListener)
            adMobUtils!!.loadFullScreenAd()
        } else {
            workAfterAdActivity()
        }
    }
}