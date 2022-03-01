package com.stream.jmxplayer.model.db

import androidx.room.*
import com.stream.jmxplayer.model.PlayerModel

@Dao
interface PlayerModelDAO {
    @Query("SELECT * FROM playerModel WHERE (streamType != ${PlayerModel.STREAM_M3U} AND streamType != ${PlayerModel.WEB_VIDEO})")
    suspend fun getAll(): List<PlayerModel>

    @Query("SELECT * FROM playerModel WHERE streamType = ${PlayerModel.STREAM_M3U}")
    suspend fun getAllM3U(): List<PlayerModel>

    @Query("SELECT * FROM playerModel WHERE streamType = ${PlayerModel.WEB_VIDEO}")
    suspend fun getAllWebVideo(): List<PlayerModel>

    @Query("SELECT * FROM playerModel WHERE id = (:playerId)")
    suspend fun loadByID(playerId: Long): PlayerModel

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(playerModel: PlayerModel): Long

    @Query("SELECT EXISTS(SELECT * FROM playerModel where id=(:playerId))")
    fun contains(playerId: Long): Boolean

    @Update
    suspend fun updateModel(vararg playerModel: PlayerModel)

    @Delete
    fun deleteModel(vararg playerModel: PlayerModel)

    @Query("DELETE FROM playerModel WHERE (streamType != ${PlayerModel.STREAM_M3U} AND streamType != ${PlayerModel.WEB_VIDEO})")
    suspend fun deleteAll()

    @Query("DELETE FROM playerModel WHERE streamType = ${PlayerModel.WEB_VIDEO}")
    suspend fun deleteWebModels()

}