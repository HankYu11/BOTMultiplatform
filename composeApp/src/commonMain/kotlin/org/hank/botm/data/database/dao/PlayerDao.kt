package org.hank.botm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.hank.botm.data.database.model.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM player WHERE gameId = :gameId")
    fun getPlayersByGame(gameId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM player " +
            "WHERE gameId = (select MAX(gameId) FROM player)")
    fun getCurrentGamePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM player WHERE id = :id")
    suspend fun getPlayer(id: Long): PlayerEntity

    @Query("UPDATE player SET balance = balance + :profit WHERE id = :playerId")
    suspend fun updatePlayerBalance(playerId: Long, profit: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)
}