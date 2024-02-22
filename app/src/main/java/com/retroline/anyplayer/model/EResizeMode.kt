package com.retroline.anyplayer.model

enum class EResizeMode(var valueStr: String, val value: Int) {
    UNDEFINE("UNDEFINE", -1), FIT("Fit", 1), FILL("Fill", 2), ZOOM("Zoom", 3);

    companion object {
        fun next(value: String?): EResizeMode {
            if (value == null) return UNDEFINE
            return if (value === "Fit") FILL else if (value === "Fill") ZOOM else FIT
        }

        operator fun get(value: String?): EResizeMode {
            if (value == null) {
                return UNDEFINE
            }
            val arr: Array<EResizeMode> = values()
            for (valNow in arr) {
                if (valNow.valueStr.equals(value.trim { it <= ' ' }, ignoreCase = true)) {
                    return valNow
                }
            }
            return UNDEFINE
        }

        operator fun get(value: Int?): EResizeMode {
            if (value == null) {
                return UNDEFINE
            }
            val arr: Array<EResizeMode> = values()
            for (valNow in arr) {
                if (valNow.value == value) {
                    return valNow
                }
            }
            return UNDEFINE
        }
    }
}