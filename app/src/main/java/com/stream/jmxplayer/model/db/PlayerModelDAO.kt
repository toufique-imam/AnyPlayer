package com.stream.jmxplayer.model.db

import androidx.room.*
import com.stream.jmxplayer.model.PlayerModel

@Dao
interface PlayerModelDAO {
    @Query("SELECT * FROM playerModel WHERE streamType != ${PlayerModel.STREAM_M3U}")
    suspend fun getAll(): List<PlayerModel>

    @Query("SELECT * FROM playerModel WHERE streamType = ${PlayerModel.STREAM_M3U}")
    suspend fun getAllM3U(): List<PlayerModel>

    @Query("SELECT * FROM playerModel WHERE id = (:playerId)")
    suspend fun loadByID(playerId: Long): PlayerModel

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(playerModel: PlayerModel)

    @Update
    suspend fun updateModel(vararg playerModel: PlayerModel)

    @Delete
    fun deleteModel(vararg playerModel: PlayerModel)

    @Query("DELETE FROM playerModel")
    suspend fun deleteAll()
}