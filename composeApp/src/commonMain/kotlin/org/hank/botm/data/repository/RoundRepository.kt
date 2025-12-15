package org.hank.botm.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.hank.botm.Config
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.database.model.ResultEntity
import org.hank.botm.data.database.model.RoundEntity
import org.hank.botm.data.network.api.RoundApi
import org.hank.botm.data.network.model.toEntity
import org.hank.botm.data.sync.SyncManager
import org.hank.botm.domain.model.Result

interface RoundRepository {
    suspend fun insertRound(gameId: Int, bet: Int, results: List<Result>): kotlin.Result<Unit>
}

class RoundRepositoryImpl(
    private val appDatabase: AppDatabase,
    private val gameDao: GameDao,
    private val roundDao: RoundDao,
    private val playerDao: PlayerDao,
    private val resultDao: ResultDao,
    private val roundApi: RoundApi,
    private val syncManager: SyncManager,
    private val ioDispatcher: CoroutineDispatcher
) : RoundRepository {

    override suspend fun insertRound(
        gameId: Int,
        bet: Int,
        results: List<Result>,
    ) = withContext(ioDispatcher) {
        when {
            // Pure offline mode - not eligible for online
            !Config.isEligibleForOnline -> {
                insertRoundOffline(gameId, bet, results, isDirty = false)
            }
            // Eligible and connected - try online first
            syncManager.canUseOnline() -> {
                insertRoundOnline(gameId, bet, results).recoverCatching {
                    // Fallback to offline with dirty flag if online fails
                    insertRoundOffline(gameId, bet, results, isDirty = true).getOrThrow()
                }
            }
            // Eligible but not connected - save locally with dirty flag
            else -> {
                insertRoundOffline(gameId, bet, results, isDirty = true)
            }
        }
    }

    private suspend fun insertRoundOnline(
        gameId: Int,
        bet: Int,
        results: List<Result>,
    ): kotlin.Result<Unit> {
        return try {
            val roundDetails = roundApi.createRound(gameId = gameId, bet = bet, results = results)

            appDatabase.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    // Round entity with server ID
                    val roundEntity = RoundEntity(
                        id = roundDetails.round.id,
                        bet = roundDetails.round.bet,
                        gameId = roundDetails.round.gameId,
                        serverId = roundDetails.round.id,
                        isDirty = false
                    )
                    roundDao.insertRound(roundEntity)

                    // Update players with server data
                    val playerEntities = roundDetails.players.map { it.toEntity() }
                    playerDao.updatePlayers(playerEntities)

                    // Insert results with server IDs
                    val resultEntities = roundDetails.results.map { dto ->
                        ResultEntity(
                            id = dto.id,
                            roundId = dto.roundId,
                            playerId = dto.playerId,
                            profit = dto.profit,
                            serverId = dto.id,
                            isDirty = false
                        )
                    }
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
        isDirty: Boolean
    ): kotlin.Result<Unit> {
        return try {
            // Get current players to update their balances
            val currentPlayers = playerDao.getPlayersByGameId(gameId)

            // Create round entity (Room will autoGenerate the ID)
            val roundEntity = RoundEntity(
                bet = bet,
                gameId = gameId,
                isDirty = isDirty
            )

            appDatabase.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    // Insert round and get generated ID
                    val roundId = roundDao.insertRound(roundEntity).toInt()

                    // Create result entities
                    val resultEntities = results.map { result ->
                        ResultEntity(
                            roundId = roundId,
                            playerId = result.playerId,
                            profit = result.profit,
                            isDirty = isDirty
                        )
                    }

                    // Update player balances based on profits
                    val updatedPlayers = currentPlayers.map { player ->
                        val profit = results.find { it.playerId == player.id }?.profit ?: 0
                        player.copy(
                            balance = player.balance + profit,
                            isDirty = isDirty
                        )
                    }

                    playerDao.updatePlayers(updatedPlayers)
                    resultDao.insertResults(resultEntities)

                    // Mark the game as dirty if this round is dirty
                    if (isDirty) {
                        gameDao.markDirty(gameId)
                    }
                }
            }
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}