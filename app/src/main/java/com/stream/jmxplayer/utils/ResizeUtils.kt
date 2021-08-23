package com.stream.jmxplayer.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.stream.jmxplayer.model.EAspectRatio
import com.stream.jmxplayer.model.EResizeMode

class ResizeUtils(val activity: Activity) {
    var aspectRatio: EAspectRatio = EAspectRatio.ASPECT_MATCH
    var resizeMode: EResizeMode = EResizeMode.FIT

    fun changeResize() {
        resizeMode = EResizeMode.next(resizeMode.valueStr)
        GlobalFunctions.toaster(activity, resizeMode.valueStr)
    }

    fun changeAspectRatio() {
        aspectRatio = EAspectRatio.next(aspectRatio.valueStr)
        GlobalFunctions.toaster(activity, aspectRatio.valueStr)
    }

    fun setResize(): Int {
        return when (resizeMode) {
            EResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            EResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    fun setAspectRatio(requestedOrientation: Int): FrameLayout.LayoutParams {
        val width = GlobalFunctions.getScreenWidth(activity)
        val height = GlobalFunctions.getScreenHeight(activity)

        val orientationNow = requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val params: FrameLayout.LayoutParams

        when (aspectRatio) {
            EAspectRatio.ASPECT_1_1 -> {
                params = if (orientationNow) {
                    FrameLayout.LayoutParams(width, width)
                } else {
                    FrameLayout.LayoutParams(height, height)
                }

            }
            EAspectRatio.ASPECT_4_3 -> {
                params = if (orientationNow) {
                    FrameLayout.LayoutParams(width, (3 * width) / 4)
                } else {
                    FrameLayout.LayoutParams((height * 4) / 3, height)
                }
            }
            EAspectRatio.ASPECT_16_9 -> {
                params = if (orientationNow) {
                    FrameLayout.LayoutParams(width, (16 * width) / 9)
                } else {
                    FrameLayout.LayoutParams((height * 9) / 16, height)
                }
            }
            EAspectRatio.ASPECT_MATCH -> {
                params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            else -> {
                params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
        params.gravity = Gravity.CENTER
        return params
    }

}