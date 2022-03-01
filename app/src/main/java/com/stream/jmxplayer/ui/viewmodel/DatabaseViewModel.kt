package com.stream.jmxplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stream.jmxplayer.model.PlayerModel
import com.stream.jmxplayer.model.db.HistoryDatabase
import kotlinx.coroutines.launch

class DatabaseViewModel(application: Application) : AndroidViewModel(application) {
    val database = HistoryDatabase.getInstance(application)
    private val _videos = MutableLiveData<List<PlayerModel>>()
    val videos: LiveData<List<PlayerModel>> get() = _videos
    fun getAll() {
        viewModelScope.launch {
            _videos.postValue(database.playerModelDao().getAll())
        }
    }

    fun deleteModel(playerModel: PlayerModel) {
        viewModelScope.launch {
            database.playerModelDao().deleteModel(playerModel)
        }

    }

    fun deleteAll() {
        viewModelScope.launch {
            database.playerModelDao().deleteAll()
            _videos.postValue(emptyList())
        }
    }

    fun getAllM3U() {
        viewModelScope.launch {
            _videos.postValue(database.playerModelDao().getAllM3U())
        }
    }

    fun insertModel(playerModel: PlayerModel) {
        if (playerModel.streamType == PlayerModel.WEB_VIDEO) playerModel.streamType =
            PlayerModel.STREAM_ONLINE_GENERAL
        viewModelScope.launch {
            database.playerModelDao().insertModel(playerModel)
        }
    }
}