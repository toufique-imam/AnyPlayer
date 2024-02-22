package com.retroline.anyplayer.model

import androidx.annotation.Keep

@Keep
interface IResultListener {
    fun workResult(result: Any)
}