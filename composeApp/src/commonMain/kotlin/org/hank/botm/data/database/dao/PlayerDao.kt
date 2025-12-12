package org.hank.botm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import org.hank.botm.data.database.model.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Update
    suspend fun updatePlayers(players: List<PlayerEntity>)

    @Query("SELECT * FROM player WHERE gameId = :gameId")
    suspend fun getPlayersByGameId(gameId: Int): List<PlayerEntity>

    // Sync-related queries
    @Query("UPDATE player SET serverId = :serverId WHERE id = :playerId")
    suspend fun updateServerId(playerId: Int, serverId: Int)

    @Query("UPDATE player SET balance = :balance WHERE serverId = :serverId")
    suspend fun updateBalanceByServerId(serverId: Int, balance: Int)

    @Query("SELECT id, serverId FROM player WHERE gameId = :gameId AND serverId IS NOT NULL")
    suspend fun getIdMappingsByGameId(gameId: Int): List<PlayerIdMapping>
}

/**
 * Data class for player ID mappings (local ID to server ID).
 */
data class PlayerIdMapping(
    val id: Int,
    val serverId: Int
)