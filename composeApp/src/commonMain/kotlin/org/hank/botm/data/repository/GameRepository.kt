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
import org.hank.botm.currentTimeMillis
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
import org.hank.botm.data.network.model.toEntity
import org.hank.botm.data.sync.SyncManager
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
    private val syncManager: SyncManager,
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
            when {
                // Pure offline mode - not eligible for online
                !Config.isEligibleForOnline -> {
                    createGameOffline(createGame, isDirty = false)
                }
                // Eligible and connected - try online first
                syncManager.canUseOnline() -> {
                    createGameOnline(createGame).recoverCatching {
                        // Fallback to offline with dirty flag if online fails
                        createGameOffline(createGame, isDirty = true).getOrThrow()
                    }
                }
                // Eligible but not connected - save locally with dirty flag
                else -> {
                    createGameOffline(createGame, isDirty = true)
                }
            }
        }

    private suspend fun createGameOnline(createGame: CreateGame): Result<Unit> {
        return try {
            val gameWithPlayers = gameApi.createGame(createGame.toDto())
            val now = currentTimeMillis()

            appDatabase.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    // Create game entity with server ID (id = serverId in new model)
                    val gameEntity = GameEntity(
                        id = gameWithPlayers.game.id,
                        isDirty = false,
                        lastSyncedAt = now,
                        createdAt = now
                    )
                    gameDao.insertGame(gameEntity)

                    // Create player entities with server IDs
                    val playerEntities = gameWithPlayers.players.map { dto ->
                        PlayerEntity(
                            id = dto.id,
                            name = dto.name,
                            balance = dto.balance,
                            gameId = dto.gameId
                        )
                    }
                    playerDao.insertPlayers(playerEntities)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createGameOffline(createGame: CreateGame, isDirty: Boolean): Result<Unit> {
        return try {
            val now = currentTimeMillis()

            // Use Room autoGenerate - id = 0 tells Room to generate
            val gameEntity = GameEntity(
                id = 0,
                isDirty = isDirty,
                createdAt = now
            )

            appDatabase.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    // Insert and get generated ID
                    val gameId = gameDao.insertGameAndGetId(gameEntity).toInt()

                    // Create player entities - Room will autoGenerate IDs
                    val playerEntities = (0 until 4).map { index ->
                        PlayerEntity(
                            id = 0,
                            name = createGame.getNameByIndex(index),
                            balance = 0,
                            gameId = gameId
                        )
                    }
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
        // Step 1: If eligible for online and connected, try server first
        // This ensures we get the authoritative state from server
        if (Config.isEligibleForOnline && syncManager.canUseOnline()) {
            try {
                gameApi.headGame(gameId)
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                // Server check failed, continue to local checks
                if (Config.isDebug) {
                    println("[GameRepository] Server check failed for game $gameId: ${e.message}")
                }
            }
        }

        // Step 2: Check local database by id (primary key)
        // In the new model, id = serverId after sync
        if (gameDao.getGameById(gameId) != null) {
            return@withContext Result.success(Unit)
        }

        // Not found anywhere
        Result.failure(Exception("Game with ID $gameId not found"))
    }

    override fun subscribeToGameUpdates(gameId: Int) {
        // Skip SSE if not eligible for online or not connected
        if (!Config.isEligibleForOnline || !syncManager.canUseOnline()) {
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