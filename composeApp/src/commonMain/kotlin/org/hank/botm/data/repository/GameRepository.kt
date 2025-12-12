package org.hank.botm.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.hank.botm.Config
import org.hank.botm.OfflineIdGenerator
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.database.model.GameEntity
import org.hank.botm.data.database.model.PlayerEntity
import org.hank.botm.data.database.model.RoundEntity
import org.hank.botm.data.database.model.asDomain
import org.hank.botm.data.network.api.GameApi
import org.hank.botm.data.network.model.GameWithPlayersDto
import org.hank.botm.data.network.model.toEntity
import org.hank.botm.domain.model.CreateGame
import org.hank.botm.domain.model.Game
import org.hank.botm.domain.model.GameWithDetails
import org.hank.botm.domain.model.getNameByIndex
import org.hank.botm.domain.model.toDto

interface GameRepository {
    val game: Flow<Game?>
    val gameWithDetails: Flow<GameWithDetails>
    suspend fun createGame(createGame: CreateGame): Result<Unit>
    suspend fun clearGame()
    suspend fun checkGameExists(gameId: Int): Result<Unit>
    fun subscribeToGameUpdates(gameId: Int)
    fun disconnectFromGameUpdates()

}

class GameRepositoryImpl(
    private val appDatabase: AppDatabase,
    private val gameDao: GameDao,
    private val playerDao: PlayerDao,
    private val roundDao: RoundDao,
    private val resultDao: ResultDao,
    private val gameApi: GameApi,
    private val ioDispatcher: CoroutineDispatcher,
    private val applicationScope: CoroutineScope,
) : GameRepository {
    private var gameUpdatesJob: Job? = null
    private val subscriptionMutex = Mutex()

    override val game: Flow<Game?> = gameDao.getNewestGame()
        .map { it?.asDomain() }

    override val gameWithDetails: Flow<GameWithDetails> =
        gameDao.getNewestGameWithDetails()
            .filterNotNull()
            .map { it.asDomain() }
            .map { gameWithDetails ->
                // reverse rounds to show the newest first
                // Use asReversed() for better performance (creates a view instead of a copy)
                gameWithDetails.copy(
                    roundsWithResults = gameWithDetails.roundsWithResults.asReversed()
                )
            }

    override suspend fun createGame(createGame: CreateGame): Result<Unit> =
        withContext(ioDispatcher) {
            if (Config.offlineMode) {
                createGameOffline(createGame)
            } else {
                createGameOnline(createGame)
            }
        }

    private suspend fun createGameOnline(createGame: CreateGame): Result<Unit> {
        val gameWithPlayers: GameWithPlayersDto
        return try {
            gameWithPlayers = gameApi.createGame(createGame.toDto())
            appDatabase.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    gameDao.insertGame(gameWithPlayers.game.toEntity())
                    playerDao.insertPlayers(gameWithPlayers.players.map { it.toEntity() })
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createGameOffline(createGame: CreateGame): Result<Unit> {
        return try {
            val gameId = OfflineIdGenerator.nextGameId()
            val playerIds = OfflineIdGenerator.playerIds(gameId)

            val gameEntity = GameEntity(id = gameId)
            val playerEntities = playerIds.mapIndexed { index, playerId ->
                PlayerEntity(
                    id = playerId,
                    name = createGame.getNameByIndex(index),
                    balance = 0,
                    gameId = gameId
                )
            }

            appDatabase.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    gameDao.insertGame(gameEntity)
                    playerDao.insertPlayers(playerEntities)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearGame() {
        withContext(ioDispatcher) {
            gameDao.clearGame()
        }
    }

    override suspend fun checkGameExists(gameId: Int): Result<Unit> = withContext(ioDispatcher) {
        if (Config.offlineMode) {
            // In offline mode, check if game exists in local database
            val game = gameDao.getGameById(gameId)
            if (game != null) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Game with ID $gameId not found in local database"))
            }
        } else {
            try {
                gameApi.headGame(gameId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun subscribeToGameUpdates(gameId: Int) {
        // In offline mode, no SSE subscription needed - all data is local
        if (Config.offlineMode) {
            return
        }

        // Use applicationScope.launch to handle the mutex lock without blocking
        applicationScope.launch {
            subscriptionMutex.withLock {
                // Ensure any existing connection is closed before starting a new one
                gameUpdatesJob?.cancel()
                gameUpdatesJob?.join() // Wait for the previous job to complete

                // Launch a new coroutine to collect updates without blocking the caller
                gameUpdatesJob = applicationScope.launch {
                    gameApi.getGameFlow(gameId)
                        .collect { gameDetails ->
                            // Wrap all database operations in a transaction
                            appDatabase.useWriterConnection { transactor ->
                                transactor.immediateTransaction {
                                    gameDao.insertGame(gameDetails.game.toEntity())
                                    playerDao.insertPlayers(gameDetails.players.map { it.toEntity() })
                                    val rounds = gameDetails.roundWithResults.map {
                                        RoundEntity(
                                            id = it.roundId,
                                            bet = it.bet,
                                            gameId = gameId
                                        )
                                    }
                                    val results = gameDetails.roundWithResults
                                        .flatMap { gameRounds -> gameRounds.results.map { result -> result.toEntity() } }
                                    roundDao.insertRounds(rounds)
                                    resultDao.insertResults(results)
                                }
                            }
                        }
                }
            }
        }
    }

    override fun disconnectFromGameUpdates() {
        applicationScope.launch {
            subscriptionMutex.withLock {
                gameUpdatesJob?.cancel()
                gameUpdatesJob = null
            }
        }
    }

}