package com.retroline.anyplayer.model

enum class EAspectRatio(var valueStr: String, val value: Int) {
    UNDEFINE("UNDEFINE", 0), ASPECT_1_1("ASPECT_1_1", 1), ASPECT_16_9("ASPECT_16_9", 2), ASPECT_4_3(
        "ASPECT_4_3",
        3
    ),
    ASPECT_MATCH("ASPECT_MATCH", 4);

    companion object {
        fun next(value: String?): EAspectRatio {
            if (value == null) {
                return UNDEFINE
            }
            return when {
                value.equals(
                    "ASPECT_1_1",
                    ignoreCase = true
                ) -> ASPECT_16_9
                value.equals(
                    "ASPECT_16_9",
                    ignoreCase = true
                ) -> ASPECT_4_3
                value.equals(
                    "ASPECT_4_3",
                    ignoreCase = true
                ) -> ASPECT_MATCH
                else -> ASPECT_1_1
            }
        }

        operator fun get(value: String?): EAspectRatio {
            if (value == null) {
                return UNDEFINE
            }
            val arr: Array<EAspectRatio> = values()
            for (valNow in arr) {
                if (valNow.valueStr.equals(value.trim { it <= ' ' }, ignoreCase = true)) {
                    return valNow
                }
            }
            return UNDEFINE
        }

        operator fun get(value: Int?): EAspectRatio {
            if (value == null) {
                return UNDEFINE
            }
            val arr: Array<EAspectRatio> = values()
            for (valNow in arr) {
                if (valNow.value == value) {
                    return valNow
                }
            }
            return UNDEFINE
        }
    }
}