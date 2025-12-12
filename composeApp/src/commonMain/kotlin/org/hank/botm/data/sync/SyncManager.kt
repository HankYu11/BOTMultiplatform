package org.hank.botm.data.sync

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.hank.botm.Config
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.database.model.ResultEntity
import org.hank.botm.data.database.model.RoundEntity
import org.hank.botm.data.network.api.GameApi
import org.hank.botm.data.network.api.RoundApi
import org.hank.botm.data.network.model.CreateGameDto
import org.hank.botm.data.network.model.PlayerResultDto
import org.hank.botm.currentTimeMillis

/**
 * Manages synchronization between local database and remote server.
 * Implements offline-first strategy with local-wins conflict resolution.
 */
interface SyncManager {
    val syncState: StateFlow<SyncState>
    val connectionState: StateFlow<ConnectionState>

    /**
     * Check if online operations are possible.
     * Returns true if user is eligible for online AND currently connected.
     */
    fun canUseOnline(): Boolean

    /**
     * Perform full sync: push dirty data, then pull new data.
     */
    suspend fun performSync(): Result<Unit>

    /**
     * Mark a game as needing sync.
     */
    suspend fun markGameDirty(gameId: Int)

    /**
     * Mark a round as needing sync.
     */
    suspend fun markRoundDirty(roundId: Int)
}

class SyncManagerImpl(
    private val appDatabase: AppDatabase,
    private val gameDao: GameDao,
    private val playerDao: PlayerDao,
    private val roundDao: RoundDao,
    private val resultDao: ResultDao,
    private val gameApi: GameApi,
    private val roundApi: RoundApi,
    private val connectivityChecker: ConnectivityChecker,
    private val ioDispatcher: CoroutineDispatcher,
) : SyncManager {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override val connectionState: StateFlow<ConnectionState> = connectivityChecker.connectionState

    override fun canUseOnline(): Boolean {
        return Config.isEligibleForOnline &&
                connectionState.value == ConnectionState.Connected
    }

    override suspend fun performSync(): Result<Unit> = withContext(ioDispatcher) {
        // Skip sync if not eligible for online
        if (!Config.isEligibleForOnline) {
            return@withContext Result.success(Unit)
        }

        _syncState.value = SyncState.Syncing

        try {
            // Step 1: Check connectivity
            val isConnected = connectivityChecker.checkConnectivity()
            if (!isConnected) {
                _syncState.value = SyncState.Error("No connection", retryable = true)
                return@withContext Result.failure(Exception("No connection to server"))
            }

            // Step 2: Push dirty games (local wins strategy)
            pushDirtyGames()

            // Step 3: Pull new data from server
            pullNewData()

            val now = currentTimeMillis()
            _syncState.value = SyncState.Success(now)
            Result.success(Unit)

        } catch (e: Exception) {
            val errorMessage = e.message ?: "Sync failed"
            _syncState.value = SyncState.Error(errorMessage, retryable = true)
            if (Config.isDebug) {
                println("[SyncManager] Sync failed: $errorMessage")
                e.printStackTrace()
            }
            Result.failure(e)
        }
    }

    /**
     * Push all dirty games and their rounds to the server.
     */
    private suspend fun pushDirtyGames() {
        val dirtyGames = gameDao.getDirtyGames()

        for (game in dirtyGames) {
            try {
                if (game.serverId == null) {
                    // New game - create on server
                    pushNewGame(game.id)
                } else {
                    // Existing game - just push dirty rounds
                    pushDirtyRoundsForGame(game.id, game.serverId)
                    gameDao.clearDirtyFlag(game.id)
                }
            } catch (e: Exception) {
                if (Config.isDebug) {
                    println("[SyncManager] Failed to push game ${game.id}: ${e.message}")
                }
                // Continue with other games
            }
        }
    }

    /**
     * Create a new game on the server and update local IDs.
     */
    private suspend fun pushNewGame(localGameId: Int) {
        val players = playerDao.getPlayersByGameId(localGameId)
        if (players.size < 4) {
            if (Config.isDebug) {
                println("[SyncManager] Game $localGameId has fewer than 4 players, skipping")
            }
            return
        }

        val createGameDto = CreateGameDto(
            playerNames = players.map { it.name }
        )

        val serverGame = gameApi.createGame(createGameDto)

        // Update local records with server IDs
        appDatabase.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                // Update game with server ID
                gameDao.updateServerId(localGameId, serverGame.game.id)
                gameDao.clearDirtyFlag(localGameId)
                gameDao.updateLastSyncedAt(localGameId, currentTimeMillis())

                // Map local player IDs to server player IDs
                val serverPlayers = serverGame.players
                players.forEachIndexed { index, localPlayer ->
                    if (index < serverPlayers.size) {
                        playerDao.updateServerId(localPlayer.id, serverPlayers[index].id)
                    }
                }
            }
        }

        // Now push any dirty rounds for this game
        pushDirtyRoundsForGame(localGameId, serverGame.game.id)
    }

    /**
     * Push dirty rounds for a specific game to the server.
     */
    private suspend fun pushDirtyRoundsForGame(localGameId: Int, serverGameId: Int) {
        val dirtyRounds = roundDao.getDirtyRoundsByGameId(localGameId)
        val playerMappings = playerDao.getIdMappingsByGameId(localGameId)
        val localToServerPlayerMap = playerMappings.associate { it.id to it.serverId }

        for (round in dirtyRounds) {
            try {
                val results = resultDao.getResultsByRoundId(round.id)

                // Map local player IDs to server player IDs
                val serverResults = results.mapNotNull { result ->
                    val serverPlayerId = localToServerPlayerMap[result.playerId]
                    if (serverPlayerId != null) {
                        PlayerResultDto(
                            playerId = serverPlayerId,
                            profit = result.profit
                        )
                    } else {
                        null
                    }
                }

                if (serverResults.size != results.size) {
                    if (Config.isDebug) {
                        println("[SyncManager] Some player mappings missing for round ${round.id}")
                    }
                    continue
                }

                // Create round on server
                val roundDetails = roundApi.createRound(
                    gameId = serverGameId,
                    bet = round.bet,
                    results = serverResults.map {
                        org.hank.botm.domain.model.Result(
                            playerId = it.playerId,
                            profit = it.profit
                        )
                    }
                )

                // Update local round with server ID
                appDatabase.useWriterConnection { transactor ->
                    transactor.immediateTransaction {
                        roundDao.updateServerId(round.id, roundDetails.round.id)
                        roundDao.clearDirtyFlag(round.id)

                        // Update result server IDs
                        val serverResultsById = roundDetails.results.associateBy { it.playerId }
                        results.forEach { localResult ->
                            val serverPlayerId = localToServerPlayerMap[localResult.playerId]
                            val serverResult = serverPlayerId?.let { serverResultsById[it] }
                            if (serverResult != null) {
                                resultDao.updateServerId(localResult.id, serverResult.id)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (Config.isDebug) {
                    println("[SyncManager] Failed to push round ${round.id}: ${e.message}")
                }
                // Continue with other rounds
            }
        }
    }

    /**
     * Pull new data from server for all synced games.
     */
    private suspend fun pullNewData() {
        val syncedGames = gameDao.getSyncedGames()

        for (game in syncedGames) {
            val serverId = game.serverId ?: continue

            try {
                val serverDetails = gameApi.getGame(serverId)

                // Get existing local round server IDs to detect new rounds
                val existingServerRoundIds = roundDao.getServerIdsByGameId(game.id).toSet()

                // Build reverse player map (server ID -> local ID)
                val playerMappings = playerDao.getIdMappingsByGameId(game.id)
                val serverToLocalPlayerMap = playerMappings.associate { it.serverId to it.id }

                appDatabase.useWriterConnection { transactor ->
                    transactor.immediateTransaction {
                        // Update player balances from server
                        serverDetails.players.forEach { serverPlayer ->
                            playerDao.updateBalanceByServerId(serverPlayer.id, serverPlayer.balance)
                        }

                        // Insert new rounds from server (ones we don't have locally)
                        serverDetails.roundWithResults
                            .filter { it.roundId !in existingServerRoundIds }
                            .forEach { serverRound ->
                                // Insert the round
                                val localRoundId = roundDao.insertRound(
                                    RoundEntity(
                                        bet = serverRound.bet,
                                        gameId = game.id,
                                        serverId = serverRound.roundId,
                                        isDirty = false
                                    )
                                ).toInt()

                                // Insert results with mapped player IDs
                                val resultEntities = serverRound.results.mapNotNull { serverResult ->
                                    val localPlayerId = serverToLocalPlayerMap[serverResult.playerId]
                                    if (localPlayerId != null) {
                                        ResultEntity(
                                            roundId = localRoundId,
                                            playerId = localPlayerId,
                                            profit = serverResult.profit,
                                            serverId = serverResult.id,
                                            isDirty = false
                                        )
                                    } else {
                                        null
                                    }
                                }
                                resultDao.insertResults(resultEntities)
                            }

                        // Update last synced timestamp
                        gameDao.updateLastSyncedAt(game.id, currentTimeMillis())
                    }
                }
            } catch (e: Exception) {
                if (Config.isDebug) {
                    println("[SyncManager] Failed to pull data for game ${game.id}: ${e.message}")
                }
                // Continue with other games
            }
        }
    }

    override suspend fun markGameDirty(gameId: Int) = withContext(ioDispatcher) {
        gameDao.markDirty(gameId)
    }

    override suspend fun markRoundDirty(roundId: Int) = withContext(ioDispatcher) {
        roundDao.markDirty(roundId)
    }
}
