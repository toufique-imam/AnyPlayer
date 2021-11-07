package com.stream.jmxplayer.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.TrackInfo
import com.stream.jmxplayer.ui.view.IjkVideoView
import com.stream.jmxplayer.utils.ijkplayer.Settings
import tv.danmaku.ijk.media.player.misc.ITrackInfo
import java.util.*
import kotlin.collections.ArrayList

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