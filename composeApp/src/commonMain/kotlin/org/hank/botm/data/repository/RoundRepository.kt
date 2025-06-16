package org.hank.botm.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.database.model.RoundWithResultsEntity
import org.hank.botm.domain.model.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.hank.botm.data.network.api.RoundApi
import org.hank.botm.data.network.model.toEntity

interface RoundRepository {
    suspend fun insertRound(gameId: Int, bet: Int, results: List<Result>)
    fun getGameRoundWithResults(gameId: Int): Flow<List<RoundWithResultsEntity>>
}

class RoundRepositoryImpl (
    private val appDatabase: AppDatabase,
    private val roundDao: RoundDao,
    private val playerDao: PlayerDao,
    private val resultDao: ResultDao,
    private val roundApi: RoundApi,
    private val ioDispatcher: CoroutineDispatcher
) : RoundRepository {

    override suspend fun insertRound(
        gameId: Int,
        bet: Int,
        results: List<Result>,
    ) = withContext(ioDispatcher) {
        val roundDetails = roundApi.createRound(gameId = gameId, bet = bet, results = results)
        val roundEntity = roundDetails.round.toEntity()
        val playerEntities = roundDetails.players.map { it.toEntity() }
        val resultEntities = roundDetails.results.map { it.toEntity() }
        appDatabase.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                roundDao.insertRound(roundEntity)
                playerDao.updatePlayers(playerEntities)
                resultDao.insertResults(resultEntities)
            }
        }
    }

    override fun getGameRoundWithResults(gameId: Int): Flow<List<RoundWithResultsEntity>> {
        return roundDao.getAllNewestGameRoundWithResults(gameId)
    }
}