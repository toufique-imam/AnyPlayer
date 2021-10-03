package com.stream.jmxplayer.model.db

import androidx.room.*
import com.stream.jmxplayer.model.PlayerModel

@Dao
interface PlayerModelDAO {
    @Query("SELECT * FROM playerModel")
    fun getAll(): List<PlayerModel>

    @Query("SELECT * FROM playerModel WHERE streamType = ${PlayerModel.STREAM_M3U}")
    fun getAllM3U(): List<PlayerModel>

    @Query("SELECT * FROM playerModel WHERE id = (:playerId)")
    fun loadByID(playerId: Long): PlayerModel

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertModel(playerModel: PlayerModel)

    @Update
    fun updateModel(vararg playerModel: PlayerModel)

    @Delete
    fun deleteModel(vararg playerModel: PlayerModel)

    @Query("DELETE FROM playerModel")
    fun deleteAll()
}