package com.retroline.anyplayer.utils

import android.content.Context
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import com.retroline.anyplayer.R

class MAnimationUtils(private var context: Context) {
    lateinit var animationInLeft: Animation
    lateinit var animationOutLeft: Animation
    lateinit var animationInRight: Animation
    lateinit var animationOutRight: Animation
    lateinit var animationInMid: Animation
    lateinit var animationOutMid: Animation

    init {
        initAnimation()
    }

    private fun initAnimation() {

        animationInLeft = AnimationUtils.loadAnimation(context, R.anim.scale_in_left)
        animationOutLeft = AnimationUtils.loadAnimation(context, R.anim.scale_out_left)
        animationInLeft.fillAfter = true
        animationOutLeft.fillAfter = true
        animationInRight = AnimationUtils.loadAnimation(context, R.anim.scale_in_right)
        animationOutRight = AnimationUtils.loadAnimation(context, R.anim.scale_out_right)

        animationInRight.fillAfter = true
        animationOutRight.fillAfter = true

        animationInMid = AnimationUtils.loadAnimation(context, R.anim.scale_in_mid)
        animationOutMid = AnimationUtils.loadAnimation(context, R.anim.scale_out_mid)

        animationInMid.fillAfter = true
        animationOutMid.fillAfter = false
    }

    private fun setFocusOnExoControl(
        view: ImageView,
        animation_in: Animation, animation_out: Animation,
        imageInID: Int, imageOutID: Int
    ) {
        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                view.startAnimation(animation_in)
                view.setImageResource(imageInID)
            } else {
                view.startAnimation(animation_out)
                view.setImageResource(imageOutID)
            }
        }
    }


    fun setMidFocusExoControl(
        view: ImageView,
        imageInID: Int, imageOutID: Int
    ) {
        setFocusOnExoControl(view, animationInMid, animationInMid, imageInID, imageOutID)
    }

}