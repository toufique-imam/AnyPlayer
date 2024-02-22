package com.retroline.anyplayer.model

import androidx.activity.result.ActivityResultLauncher


interface IStoragePermission {
    fun getRequestPermissions() : ActivityResultLauncher<Array<String>>?
}