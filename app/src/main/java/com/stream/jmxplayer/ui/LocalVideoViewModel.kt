package com.stream.jmxplayer.ui

import android.app.Application
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.utils.MediaFileUtils
import kotlinx.coroutines.launch

class LocalVideoViewModel(application: Application) : AndroidViewModel(application) {
    private val _videos = MutableLiveData<List<PlayerModel>>()
    val videos: LiveData<List<PlayerModel>> get() = _videos
    private var contentObserver: ContentObserver? = null

    fun loadMedia(type: Int) {
        viewModelScope.launch {
            val videoList = MediaFileUtils.getAllMediaData(type, getApplication())
            _videos.postValue(videoList)
            val collection = MediaFileUtils.getCollection(type)
            if (contentObserver == null) {
                contentObserver = getApplication<Application>()
                    .contentResolver.registerObserver(collection) {
                        loadMedia(type)
                    }
            }
        }
    }
}

private fun ContentResolver.registerObserver(
    uri: Uri,
    observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler(Looper.myLooper()!!)) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}