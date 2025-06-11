package org.hank.botm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.hank.botm.data.database.model.PlayerEntity
import org.hank.botm.data.database.model.ResultEntity
import org.hank.botm.data.database.model.RoundEntity
import org.hank.botm.data.database.model.RoundWithResults
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Transaction
    @Query(
        """
            SELECT * FROM round
            WHERE round.gameId = :gameId
            ORDER BY round.id DESC 
        """
    )
    fun getAllNewestGameRoundWithResults(gameId: Long): Flow<List<RoundWithResults>>

    @Transaction
    suspend fun insertRoundWithRelations(
        round: RoundEntity,
        results: List<ResultEntity>,
        players: List<PlayerEntity>,
        playerDao: PlayerDao,
        resultDao: ResultDao
    ) {
        val roundId = insertRound(round)
        val resultsWithRoundId = results.map { result ->
            // update players
            playerDao.updatePlayerBalance(result.playerId, result.profit)
            // add roundId to results
            result.copy(roundId = roundId)
        }
        resultDao.insertResults(resultsWithRoundId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: RoundEntity): Long
}