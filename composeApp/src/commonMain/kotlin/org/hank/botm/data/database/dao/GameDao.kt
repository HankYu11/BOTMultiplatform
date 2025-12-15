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

    @Query("SELECT * FROM game WHERE id = :gameId")
    suspend fun getGameById(gameId: Int): GameEntity?

    @Query("SELECT * FROM game WHERE serverId = :serverId")
    suspend fun getGameByServerId(serverId: Int): GameEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGame(gameEntity: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameAndGetId(gameEntity: GameEntity): Long

    // We clear all related tables when deleting a game with relations defined in entities
    @Query("DELETE FROM game")
    suspend fun clearGame()

    // Sync-related queries
    @Query("SELECT * FROM game WHERE isDirty = 1")
    suspend fun getDirtyGames(): List<GameEntity>

    @Query("SELECT * FROM game WHERE serverId IS NOT NULL")
    suspend fun getSyncedGames(): List<GameEntity>

    @Query("UPDATE game SET isDirty = 1 WHERE id = :gameId")
    suspend fun markDirty(gameId: Int)

    @Query("UPDATE game SET isDirty = 0 WHERE id = :gameId")
    suspend fun clearDirtyFlag(gameId: Int)

    @Query("UPDATE game SET serverId = :serverId WHERE id = :gameId")
    suspend fun updateServerId(gameId: Int, serverId: Int)

    @Query("UPDATE game SET lastSyncedAt = :timestamp WHERE id = :gameId")
    suspend fun updateLastSyncedAt(gameId: Int, timestamp: Long)
}