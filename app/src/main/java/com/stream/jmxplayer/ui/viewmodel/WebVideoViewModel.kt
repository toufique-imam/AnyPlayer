package com.stream.jmxplayer.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.jmxplayer.model.PlayerModel
import kotlinx.coroutines.launch

class WebVideoViewModel : ViewModel() {
    private val _videos = MutableLiveData<ArrayList<PlayerModel>>()
    val webVideo = ArrayList<PlayerModel>()
    val videos: LiveData<ArrayList<PlayerModel>> get() = _videos
    val ids = HashSet<Long>()

    fun getDownloadData() {
        viewModelScope.launch {
            _videos.postValue(webVideo)
        }
    }

    fun addDownloadModel(playerModel: PlayerModel): Boolean {
        if (ids.contains(playerModel.id)) return false
        viewModelScope.launch {
            ids.add(playerModel.id)
            webVideo.add(playerModel)
            _videos.postValue(webVideo)
        }
        return true
    }

    fun clearDownloadModel() {
        viewModelScope.launch {
            webVideo.clear()
            ids.clear()
            _videos.postValue(webVideo)
        }
    }

}