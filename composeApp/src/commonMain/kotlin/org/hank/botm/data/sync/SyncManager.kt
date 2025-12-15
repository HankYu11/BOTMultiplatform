package org.hank.botm.data.sync

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.hank.botm.data.database.model.ResultEntity
import org.hank.botm.data.database.model.RoundEntity
import org.hank.botm.data.network.api.GameApi
import org.hank.botm.data.network.api.RoundApi
import org.hank.botm.data.network.model.SyncGameRequest
import org.hank.botm.data.network.model.SyncPlayerRequest
import org.hank.botm.data.network.model.SyncResultRequest
import org.hank.botm.data.network.model.SyncRoundRequest

/**
 * Manages synchronization between local database and remote server.
 * Implements "sync-and-replace" strategy: upload complete game data,
 * then replace local data with server response (server is source of truth).
 */
interface SyncManager {
    val syncState: StateFlow<SyncState>
    val connectionState: StateFlow<ConnectionState>

    /**
     * Emits when a game ID changes after sync (oldId to newId).
     * Used for navigation handling when local IDs are replaced with server IDs.
     */
    val gameIdChanged: SharedFlow<Pair<Int, Int>>

    /**
     * Check if online operations are possible.
     * Returns true if user is eligible for online AND currently connected.
     */
    fun canUseOnline(): Boolean

    /**
     * Perform full sync: upload dirty games and replace with server data.
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

    private val _gameIdChanged = MutableSharedFlow<Pair<Int, Int>>()
    override val gameIdChanged: SharedFlow<Pair<Int, Int>> = _gameIdChanged.asSharedFlow()

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

            // Step 2: Sync dirty games using sync-and-replace strategy
            syncDirtyGames()

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
     * Sync all dirty games using the sync-and-replace strategy.
     * For each dirty game:
     * 1. Collect all local data (players, rounds, results)
     * 2. Build sync request using player indices (not IDs)
     * 3. Send to server
     * 4. Delete local data and insert server response data
     */
    private suspend fun syncDirtyGames() {
        val dirtyGames = gameDao.getDirtyGames()

        for (game in dirtyGames) {
            try {
                syncGame(game)
            } catch (e: Exception) {
                if (Config.isDebug) {
                    println("[SyncManager] Failed to sync game ${game.id}: ${e.message}")
                }
                // Continue with other games
            }
        }
    }

    /**
     * Sync a single dirty game to the server.
     */
    private suspend fun syncGame(game: GameEntity) {
        val localGameId = game.id

        // 1. Collect all local data
        val players = playerDao.getPlayersByGameId(localGameId)
        if (players.size < 4) {
            if (Config.isDebug) {
                println("[SyncManager] Game $localGameId has fewer than 4 players, skipping")
            }
            return
        }

        val rounds = roundDao.getRoundsByGameId(localGameId)

        // Build player ID to index map for result mapping
        val playerIdToIndex = players.mapIndexed { index, player -> player.id to index }.toMap()

        // 2. Build sync request
        val syncRequest = SyncGameRequest(
            players = players.map { SyncPlayerRequest(name = it.name) },
            rounds = rounds.map { round ->
                val results = resultDao.getResultsByRoundId(round.id)
                SyncRoundRequest(
                    bet = round.bet,
                    results = results.mapNotNull { result ->
                        val playerIndex = playerIdToIndex[result.playerId]
                        if (playerIndex != null) {
                            SyncResultRequest(
                                playerIndex = playerIndex,
                                profit = result.profit
                            )
                        } else {
                            null
                        }
                    }
                )
            }
        )

        // 3. Send to server
        val serverGame = gameApi.syncGame(syncRequest)
        val serverGameId = serverGame.game.id

        // 4. Replace local data with server data
        val now = currentTimeMillis()

        appDatabase.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                // Delete local game (cascades to players, rounds, results)
                gameDao.deleteById(localGameId)

                // Insert server game
                gameDao.insertGame(
                    GameEntity(
                        id = serverGameId,
                        isDirty = false,
                        lastSyncedAt = now,
                        createdAt = game.createdAt
                    )
                )

                // Insert server players
                val playerEntities = serverGame.players.map { dto ->
                    PlayerEntity(
                        id = dto.id,
                        name = dto.name,
                        balance = dto.balance,
                        gameId = serverGameId
                    )
                }
                playerDao.insertPlayers(playerEntities)

                // Insert server rounds and results
                serverGame.roundWithResults.forEach { roundDto ->
                    roundDao.insertRound(
                        RoundEntity(
                            id = roundDto.roundId,
                            bet = roundDto.bet,
                            gameId = serverGameId,
                            isDirty = false
                        )
                    )

                    val resultEntities = roundDto.results.map { resultDto ->
                        ResultEntity(
                            id = resultDto.id,
                            roundId = roundDto.roundId,
                            playerId = resultDto.playerId,
                            profit = resultDto.profit
                        )
                    }
                    resultDao.insertResults(resultEntities)
                }
            }
        }

        // 5. Notify ID changed (for navigation handling)
        if (localGameId != serverGameId) {
            _gameIdChanged.emit(localGameId to serverGameId)
        }

        if (Config.isDebug) {
            println("[SyncManager] Successfully synced game $localGameId -> $serverGameId")
        }
    }

    override suspend fun markGameDirty(gameId: Int) = withContext(ioDispatcher) {
        gameDao.markDirty(gameId)
    }

    override suspend fun markRoundDirty(roundId: Int) = withContext(ioDispatcher) {
        roundDao.markDirty(roundId)
    }
}
