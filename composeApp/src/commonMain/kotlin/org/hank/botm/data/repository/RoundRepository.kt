package org.hank.botm.data.repository

import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.database.model.RoundWithResults
import org.hank.botm.domain.model.Player
import org.hank.botm.domain.model.Result
import org.hank.botm.domain.model.Round
import org.hank.botm.domain.model.asEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface RoundRepository {
    suspend fun insertRound(round: Round, results: List<Result>, players: List<Player>)
    fun getGameRoundWithResults(gameId: Int): Flow<List<RoundWithResults>>
}

class RoundRepositoryImpl (
    private val roundDao: RoundDao,
    private val playerDao: PlayerDao,
    private val resultDao: ResultDao,
    private val ioDispatcher: CoroutineDispatcher
) : RoundRepository {

    override suspend fun insertRound(
        round: Round,
        results: List<Result>,
        players: List<Player>
    ) = withContext(ioDispatcher) {
        roundDao.insertRoundWithRelations(
            round.asEntity(),
            results.map { it.asEntity(round.id) },
            players.map { it.asEntity() },
            playerDao,
            resultDao
        )
    }

    override fun getGameRoundWithResults(gameId: Int): Flow<List<RoundWithResults>> {
        return roundDao.getAllNewestGameRoundWithResults(gameId)
    }
}