package com.stream.jmxplayer.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.IResultListener
import com.stream.jmxplayer.model.PlayerModel

class DownloaderUtils(val activity: Activity, var playerModel: PlayerModel) {
    private var downloader: String = ""
    fun downloadVideo(stateNow: Int) {
        if (stateNow == 1) {
            downloadWithIDM()
        } else if (stateNow == 2) {
            downloadWithADM()
        }
    }

    fun updatePlayerModel(playerModel: PlayerModel) {
        this.playerModel = playerModel
    }

    fun showDownloadDialog(hideSystemUi: () -> Unit, resultListener: IResultListener) {
        val dialogueView: View =
            activity.layoutInflater.inflate(R.layout.custom_dialogue_download, null)
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setTitle("Descargar con")
        builder.setView(dialogueView)
        val positiveButton: Button = dialogueView.findViewById(R.id.button_exit_ac)
        val negativeButton: Button = dialogueView.findViewById(R.id.button_exit_wa)

        val alertDialog: AlertDialog = builder.create()
        val radioGroup: RadioGroup = dialogueView.findViewById(R.id.radio_group_download)

        downloader = ""

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val radioButtonNow: RadioButton = group.findViewById(checkedId)
            downloader = if (radioButtonNow.text.toString().trim() == "ADM") {
                "ADM"
            } else {
                "IDM"
            }
        }
        positiveButton.setOnClickListener {
            if (downloader.isEmpty()) {
                GlobalFunctions.toaster(activity, "Please Select an app to download")
            } else {
                resultListener.workResult(downloader)
                //btnClick = downloader
                alertDialog.dismiss()
                //goAction()
            }
        }
        negativeButton.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.setOnDismissListener {
            hideSystemUi()
        }

        alertDialog.show()
    }

    private fun downloadWithADM() {
        val appInstalled = GlobalFunctions.appInstalledOrNot(activity, "com.dv.adm")
        val appInstalled2 = GlobalFunctions.appInstalledOrNot(activity, "com.dv.adm.pay")
        val appInstalled3 = GlobalFunctions.appInstalledOrNot(activity, "com.dv.adm.old")

        val str3: String
        if (appInstalled || appInstalled2 || appInstalled3) {
            str3 = when {
                appInstalled2 -> {
                    "com.dv.adm.pay"
                }
                appInstalled -> {
                    "com.dv.adm"
                }
                else -> {
                    "com.dv.adm.old"
                }
            }

            downloadAction(str3)

        } else {
            str3 = "com.dv.adm"
            //prompt to download ADM
            marketAction(str3)
        }
    }

    private fun downloadAction(str3: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(playerModel.link), "application/x-mpegURL")
            intent.`package` = str3
            if (playerModel.cookies.isNotEmpty()) {
                intent.putExtra("Cookie", playerModel.cookies)
                intent.putExtra("cookie", playerModel.cookies)
                intent.putExtra("Cookies", playerModel.cookies)
                intent.putExtra("cookie", playerModel.cookies)
            }
            activity.startActivity(intent)
            return
        } catch (e: Exception) {
            return
        }
    }

    private fun downloadWithIDM() {
        val appInstalled =
            GlobalFunctions.appInstalledOrNot(activity, "idm.internet.download.manager")
        val str3 = "idm.internet.download.manager"
        if (appInstalled) {
            downloadAction(str3)
        } else {
            marketAction(str3)
        }
    }

    private fun marketAction(str3: String) {
        try {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$str3")
                )
            )
        } catch (e: Exception) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$str3")
                )
            )
        }
    }
}