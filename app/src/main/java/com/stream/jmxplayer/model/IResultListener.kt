package com.stream.jmxplayer.model

import androidx.annotation.Keep

@Keep
interface IResultListener {
    fun workResult(result: Any)
}