package com.stream.jmxplayer.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.jmxplayer.model.PlayerModel
import kotlinx.coroutines.launch

class WebVideoViewModel : ViewModel() {
    private val _videos = MutableLiveData<List<PlayerModel>>()
    val webVideo = ArrayList<PlayerModel>()
    val videos: LiveData<List<PlayerModel>> get() = _videos


    fun getDownloadData() {
        viewModelScope.launch {
            _videos.postValue(webVideo)
        }

    }

    fun addDownloadModel(playerModel: PlayerModel) {
        viewModelScope.launch {
            webVideo.add(playerModel)
            _videos.postValue(webVideo)
        }
    }

    fun clearDownloadModel() {
        viewModelScope.launch {
            webVideo.clear()
            _videos.postValue(webVideo)
        }
    }

}