package com.stream.jmxplayer.ui

import android.app.Application
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
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

    fun loadVideos() {
        viewModelScope.launch {
            val videoList = MediaFileUtils.getAllMovieData(getApplication())
            _videos.postValue(videoList)
            val collection = MediaFileUtils.getCollection()
            if (contentObserver == null) {
                contentObserver = getApplication<Application>()
                    .contentResolver.registerObserver(collection) {
                        loadVideos()
                    }
            }
        }
    }
}

private fun ContentResolver.registerObserver(
    uri: Uri,
    observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}