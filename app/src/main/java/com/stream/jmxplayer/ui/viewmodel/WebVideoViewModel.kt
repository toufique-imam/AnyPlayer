package com.stream.jmxplayer.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stream.jmxplayer.model.PlayerModel

class WebVideoViewModel : ViewModel() {
    private val _videos = MutableLiveData<HashSet<PlayerModel>>()
    val webVideo = HashSet<PlayerModel>()
    val videos: LiveData<HashSet<PlayerModel>> get() = _videos


    fun getDownloadData() {
        _videos.postValue(webVideo)
    }

    fun addDownloadModel(playerModel: PlayerModel) {
        webVideo.add(playerModel)
        _videos.postValue(webVideo)

    }

    fun clearDownloadModel() {
        webVideo.clear()
        _videos.postValue(webVideo)

    }

}