package org.hank.botm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.hank.botm.data.database.model.PlayerEntity
import org.hank.botm.data.database.model.ResultEntity
import org.hank.botm.data.database.model.RoundEntity
import org.hank.botm.data.database.model.RoundWithResultsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: RoundEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRounds(rounds: List<RoundEntity>)

    // Sync-related queries
    @Query("SELECT * FROM round WHERE gameId = :gameId AND isDirty = 1")
    suspend fun getDirtyRoundsByGameId(gameId: Int): List<RoundEntity>

    @Query("SELECT serverId FROM round WHERE gameId = :gameId AND serverId IS NOT NULL")
    suspend fun getServerIdsByGameId(gameId: Int): List<Int>

    @Query("UPDATE round SET isDirty = 1 WHERE id = :roundId")
    suspend fun markDirty(roundId: Int)

    @Query("UPDATE round SET isDirty = 0 WHERE id = :roundId")
    suspend fun clearDirtyFlag(roundId: Int)

    @Query("UPDATE round SET serverId = :serverId WHERE id = :roundId")
    suspend fun updateServerId(roundId: Int, serverId: Int)
}