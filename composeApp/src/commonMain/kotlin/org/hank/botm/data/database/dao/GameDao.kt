package org.hank.botm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.hank.botm.data.database.model.GameEntity

@Dao
interface GameDao {
    @Query("SELECT * FROM game WHERE id = :id")
    suspend fun fetchGame(id: Long): GameEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(gameEntity: GameEntity): Long

    @Query("SELECT * FROM game WHERE isFinished = 0")
    suspend fun fetchUnFinishedGame(): GameEntity?

    @Query("UPDATE game SET isFinished = 1 WHERE id = :gameId")
    suspend fun finishedGame(gameId: Long)
}