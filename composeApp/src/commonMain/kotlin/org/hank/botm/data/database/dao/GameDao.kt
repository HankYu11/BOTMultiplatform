package org.hank.botm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.hank.botm.data.database.model.GameEntity
import org.hank.botm.data.database.model.GameWithDetailsEntity

@Dao
interface GameDao {
    @Query("SELECT * FROM game ORDER BY id DESC LIMIT 1")
    fun getNewestGame(): Flow<GameEntity?>

    @Transaction
    @Query("SELECT * FROM game ORDER BY id DESC LIMIT 1")
    fun getNewestGameWithDetails(): Flow<GameWithDetailsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(gameEntity: GameEntity)

    // We clear all related tables when deleting a game with relations defined in entities
    @Query("DELETE FROM game")
    suspend fun clearGame()
}