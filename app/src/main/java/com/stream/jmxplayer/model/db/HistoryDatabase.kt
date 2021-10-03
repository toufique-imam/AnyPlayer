package com.stream.jmxplayer.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stream.jmxplayer.model.PlayerModel

@TypeConverters(MapConvert::class)
@Database(entities = [PlayerModel::class], version = 3)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun playerModelDao(): PlayerModelDAO

    companion object : SingletonHolder<HistoryDatabase, Context>({
        Room.databaseBuilder(it.applicationContext, HistoryDatabase::class.java, "playerModel")
            .fallbackToDestructiveMigration()
            .build()
    })
}