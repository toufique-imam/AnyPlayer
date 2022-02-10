package com.stream.jmxplayer.utils

import android.app.Activity
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.util.Patterns
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.github.javiersantos.piracychecker.PiracyChecker
import com.github.javiersantos.piracychecker.callbacks.PiracyCheckerCallback
import com.github.javiersantos.piracychecker.enums.Display
import com.github.javiersantos.piracychecker.enums.PiracyCheckerError
import com.github.javiersantos.piracychecker.enums.PirateApp
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.TrackInfo
import com.stream.jmxplayer.ui.view.IjkVideoView
import com.stream.jmxplayer.utils.GlobalFunctions.logger
import com.stream.jmxplayer.utils.GlobalFunctions.toaster
import com.stream.jmxplayer.utils.ijkplayer.Settings
import tv.danmaku.ijk.media.player.misc.ITrackInfo
import java.util.*

fun View.isVisible() = visibility == View.VISIBLE
fun View.isInvisible() = visibility == View.INVISIBLE
fun View.isGone() = visibility == View.GONE

fun View?.setVisibility(visibility: Int) {
    this?.visibility = visibility
}

fun View?.setVisible() = setVisibility(View.VISIBLE)
fun View?.setInvisible() = setVisibility(View.INVISIBLE)
fun View?.setGone() = setVisibility(View.GONE)

fun ITrackInfo.getName() = String.format(Locale.US, "# %s: %s", language, infoInline)
fun ITrackInfo.getName(idx: Int) =
    String.format(Locale.US, "# %s %d: %s", language, idx, infoInline)

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun LifecycleOwner.isStarted() = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

fun IjkVideoView.getTypeTracksCount(type: Int): Int {
    val trackInfo = trackInfo ?: return 0
    var ans = 0
    for (i in trackInfo) {
        if (i.trackType == type) ans++
    }
    return ans
}

fun IjkVideoView.getTypeTracks(type: Int): ArrayList<TrackInfo> {
    val ans = ArrayList<TrackInfo>()
    val trackInfo = trackInfo ?: return ans
    for ((idx, i) in trackInfo.withIndex()) {
        if (i.trackType == type) {
            ans.add(TrackInfo(idx, i))
        }
    }
    return ans
}

fun IjkVideoView.getVideoTracksCount(): Int {
    return getTypeTracksCount(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO)
}

fun IjkVideoView.getAudioTracksCount(): Int {
    return getTypeTracksCount(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
}

fun IjkVideoView.getSubTracksCount(): Int {
    return getTypeTracksCount(ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE)
}

fun IjkVideoView.getTimeTracksCount(): Int {
    return getTypeTracksCount(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
}

fun IjkVideoView.getVideoTracks(): ArrayList<TrackInfo> {
    return getTypeTracks(ITrackInfo.MEDIA_TRACK_TYPE_VIDEO)
}

fun IjkVideoView.getAudioTracks(): ArrayList<TrackInfo> {
    return getTypeTracks(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
}

fun IjkVideoView.getSubTracks(): ArrayList<TrackInfo> {
    return getTypeTracks(ITrackInfo.MEDIA_TRACK_TYPE_SUBTITLE)
}

fun IjkVideoView.getTimeTracks(): ArrayList<TrackInfo> {
    return getTypeTracks(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
}

fun AppCompatActivity.createPlayerDialogue(playerID: Int, yesSure: (Int) -> Unit): AlertDialog {
    val dialogView = this.layoutInflater.inflate(R.layout.dialog_tracks, null)
    val builder = AlertDialog.Builder(this).setTitle("Select Player").setView(dialogView)
    builder.setCancelable(true)
    val radioGroup: RadioGroup = dialogView.findViewById(R.id.radio_group_server)
    val playerDialog = builder.create()
    val playerList = resources.getStringArray(R.array.pref_entries_player_select)
    val playerId = arrayOf(
        Settings.PV_PLAYER__AndroidMediaPlayer,
        Settings.PV_PLAYER__IjkExoMediaPlayer,
        Settings.PV_PLAYER__IjkMediaPlayer
    )
    for (i in 1 until playerList.size) {
        val radioButton = RadioButton(this)
        radioButton.id = playerId[i - 1]
        radioButton.text = playerList[i]
        radioButton.textSize = 15f
        radioButton.setTextColor(Color.BLACK)
        if (playerId[i - 1] == playerID) {
            radioButton.isChecked = true
        }
        radioGroup.addView(radioButton)
    }
    val materialButton: MaterialButton = dialogView.findViewById(R.id.button_stream_now)


    materialButton.setOnClickListener {
        playerDialog.dismiss()
        //playerNow = radioGroup.checkedRadioButtonId
        yesSure(radioGroup.checkedRadioButtonId)
    }
    return playerDialog
}

fun Activity.createAlertDialogueLoading(): AlertDialog {
    val dialogueView =
        layoutInflater.inflate(
            R.layout.custom_dialog_loading, null
        )
    return AlertDialog.Builder(this)
        .setView(dialogueView)
        .setCancelable(false)
        .create()
}
/*
fun AppCompatActivity.areYouSureDialogue(message: String, action: () -> Unit) {
    AlertDialog.Builder(this)
        .setMessage(message)
        .setCancelable(true)
        .setPositiveButton(
            R.string.accept
        ) { _, _ ->
            action()
        }
        .show()
}
 */

fun AppCompatActivity.showProMode(rewardAdAction: () -> Unit) {
    val dialogView = layoutInflater.inflate(R.layout.custom_dialog_pro, null)
    val builder = AlertDialog.Builder(this)
        .setTitle(R.string.pro_dialog_title)
        .setView(dialogView)
    val rewardButton: MaterialButton = dialogView.findViewById(R.id.material_button_reward)
    val proButton: MaterialButton = dialogView.findViewById(R.id.material_button_buy_pro)
    val dialogNow = builder.create()
    rewardButton.setOnClickListener {
        dialogNow.dismiss()
        rewardAdAction()
    }
    proButton.setOnClickListener {
        dialogNow.dismiss()
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName.paid")
                )
            )
        } catch (e2: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName.paid")
                )
            )
        }
    }
    dialogNow.show()
}

fun Double.round(decimals: Int = 2): Double = "%.${decimals}f".format(this).toDouble()

val sdkInt = Build.VERSION.SDK_INT

enum class UiModeType {
    NORMAL, DESK, CAR, TV, APPLIANCE, WATCH, VR
}

fun Activity.getUiModeType(): UiModeType {
    val uiModeManager = getSystemService(Activity.UI_MODE_SERVICE) as UiModeManager
    val currentMode = uiModeManager.currentModeType
    when (currentMode) {
        Configuration.UI_MODE_TYPE_APPLIANCE -> return UiModeType.APPLIANCE
        Configuration.UI_MODE_TYPE_CAR -> return UiModeType.CAR
        Configuration.UI_MODE_TYPE_DESK -> return UiModeType.DESK
        Configuration.UI_MODE_TYPE_TELEVISION -> return UiModeType.TV
    }
    if (sdkInt >= Build.VERSION_CODES.KITKAT_WATCH && currentMode == Configuration.UI_MODE_TYPE_WATCH)
        return UiModeType.WATCH
    if (sdkInt >= Build.VERSION_CODES.O && currentMode == Configuration.UI_MODE_TYPE_VR_HEADSET)
        return UiModeType.VR
    if (isLikelyTelevision()) return UiModeType.TV
    return UiModeType.NORMAL
}

fun Activity.hasSystemService(serviceName: String): Boolean {
    return packageManager.hasSystemFeature(serviceName)
}

fun Activity.isLikelyTelevision(): Boolean {
    if (sdkInt >= Build.VERSION_CODES.LOLLIPOP && hasSystemService(PackageManager.FEATURE_LEANBACK)) return true
    if (sdkInt < Build.VERSION_CODES.LOLLIPOP && hasSystemService(PackageManager.FEATURE_TELEVISION)) return true
    if (isBatteryAbsent() && hasSystemService(PackageManager.FEATURE_USB_HOST) && hasSystemService(
            PackageManager.FEATURE_ETHERNET
        ) && !hasSystemService(PackageManager.FEATURE_TOUCHSCREEN)
    ) return true
    return false
}

fun Activity.isBatteryAbsent(): Boolean {
    val batteryManager = getSystemService(Activity.BATTERY_SERVICE) as BatteryManager
    return if (sdkInt >= Build.VERSION_CODES.LOLLIPOP) {
        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) == 0
    } else {
        false
    }
}

fun Activity.initPiracy(onAllow: () -> Unit): PiracyChecker {
    val piracyChecker = PiracyChecker(this)
    piracyChecker.display(Display.DIALOG)
    piracyChecker.enableUnauthorizedAppsCheck()
//    todo must check
    if (GlobalFunctions.isProVersion())
        piracyChecker.enableGooglePlayLicensing(getString(R.string.app_lvl_paid))
    else
        piracyChecker.enableGooglePlayLicensing(getString(R.string.app_lvl))

    piracyChecker.enableDebugCheck()
//    piracyChecker.enableSigningCertificates(getString(R.string.app_sign))
    val valid: String = getString(R.string.download_valid)

    val callback = object : PiracyCheckerCallback() {
        override fun allow() {
            onAllow()
        }

        override fun doNotAllow(error: PiracyCheckerError, app: PirateApp?) {
            logger("PIRACY", error.toString())
            toaster(this@initPiracy, valid)
            finish()
        }
    }
    piracyChecker.callback(callback)
    piracyChecker.start()
    return piracyChecker
}

fun String.checkUrl(): Boolean {
    return Patterns.WEB_URL.matcher(this).matches()
}

fun TextInputEditText.checkText(): Boolean {
    if (text == null || text.toString().isEmpty()) {
        return false
    }
    return true
}

fun TextInputEditText.checkUrl(): Boolean {
    if (!checkText() || !text.toString().trim().checkUrl()) {
        return false
    }
    return true
}