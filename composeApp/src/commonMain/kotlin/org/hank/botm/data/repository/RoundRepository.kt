package org.hank.botm.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.hank.botm.Config
import org.hank.botm.OfflineIdGenerator
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.database.model.ResultEntity
import org.hank.botm.data.database.model.RoundEntity
import org.hank.botm.data.network.api.RoundApi
import org.hank.botm.data.network.model.toEntity
import org.hank.botm.domain.model.Result

interface RoundRepository {
    suspend fun insertRound(gameId: Int, bet: Int, results: List<Result>): kotlin.Result<Unit>
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
        if (Config.offlineMode) {
            insertRoundOffline(gameId, bet, results)
        } else {
            insertRoundOnline(gameId, bet, results)
        }
    }

    private suspend fun insertRoundOnline(
        gameId: Int,
        bet: Int,
        results: List<Result>,
    ): kotlin.Result<Unit> {
        return try {
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
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    private suspend fun insertRoundOffline(
        gameId: Int,
        bet: Int,
        results: List<Result>,
    ): kotlin.Result<Unit> {
        return try {
            // Get current players to update their balances
            val currentPlayers = playerDao.getPlayersByGameId(gameId)

            // Create round entity (Room will autoGenerate the ID)
            val roundEntity = RoundEntity(bet = bet, gameId = gameId)

            appDatabase.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    // Insert round and get generated ID
                    val roundId = roundDao.insertRound(roundEntity).toInt()

                    // Create result entities with generated IDs and the new round ID
                    val resultEntities = results.map { result ->
                        ResultEntity(
                            id = OfflineIdGenerator.nextResultId(),
                            roundId = roundId,
                            playerId = result.playerId,
                            profit = result.profit
                        )
                    }

                    // Update player balances based on profits
                    val updatedPlayers = currentPlayers.map { player ->
                        val profit = results.find { it.playerId == player.id }?.profit ?: 0
                        player.copy(balance = player.balance + profit)
                    }

                    playerDao.updatePlayers(updatedPlayers)
                    resultDao.insertResults(resultEntities)
                }
            }
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}