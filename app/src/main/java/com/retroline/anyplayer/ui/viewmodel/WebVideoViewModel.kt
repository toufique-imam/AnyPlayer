package com.retroline.anyplayer.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.retroline.anyplayer.model.PlayerModel
import com.retroline.anyplayer.model.db.HistoryDatabase
import kotlinx.coroutines.launch

class WebVideoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = HistoryDatabase.getInstance(application)
    private val _videos = MutableLiveData<List<PlayerModel>>()
    val videos: LiveData<List<PlayerModel>> get() = _videos

    fun getDownloadData() {
        viewModelScope.launch {
            _videos.postValue(database.playerModelDao().getAllWebVideo())
        }
    }

    fun addDownloadModel(playerModel: PlayerModel): Boolean {
        if (database.playerModelDao().contains(playerId = playerModel.id)) return false
        viewModelScope.launch {
            val id = database.playerModelDao().insertModel(playerModel)
            Log.e("downloadModel", id.toString())
        }
        return true
    }

    fun clearDownloadModel() {
        viewModelScope.launch {
            database.playerModelDao().deleteWebModels()
            _videos.postValue(emptyList())
        }
    }

}