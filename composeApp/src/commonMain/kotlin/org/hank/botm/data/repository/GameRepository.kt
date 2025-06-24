package org.hank.botm.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hank.botm.data.database.AppDatabase
import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.database.model.RoundEntity
import org.hank.botm.data.database.model.asDomain
import org.hank.botm.data.network.api.GameApi
import org.hank.botm.data.network.model.GameWithPlayersDto
import org.hank.botm.data.network.model.toEntity
import org.hank.botm.domain.model.CreateGame
import org.hank.botm.domain.model.Game
import org.hank.botm.domain.model.GameWithDetails
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

    override val game: Flow<Game?> = gameDao.getNewestGame()
        .map { it?.asDomain() }

    override val gameWithDetails: Flow<GameWithDetails> =
        gameDao.getNewestGameWithDetails()
            .filterNotNull()
            .map { it.asDomain() }
            .map { gameWithDetails ->
                // reverse rounds to show the newest first
                gameWithDetails.copy(
                    roundsWithResults = gameWithDetails.roundsWithResults.reversed()
                )
            }

    override suspend fun createGame(createGame: CreateGame): Result<Unit> =
        withContext(ioDispatcher) {
            val gameWithPlayers: GameWithPlayersDto
            try {
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

    override suspend fun clearGame() {
        withContext(ioDispatcher) {
            gameDao.clearGame()
        }
    }

    override suspend fun checkGameExists(gameId: Int): Result<Unit> = withContext(ioDispatcher){
        try {
            gameApi.headGame(gameId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun subscribeToGameUpdates(gameId: Int) {
        // Ensure any existing connection is closed before starting a new one
        gameUpdatesJob?.cancel()

        // Launch a new coroutine to collect updates without blocking the caller
        gameUpdatesJob = applicationScope.launch {
            gameApi.getGameFlow(gameId)
                .collect { gameDetails ->
                    gameDao.insertGame(gameDetails.game.toEntity())
                    playerDao.insertPlayers(gameDetails.players.map { it.toEntity() })
                    val rounds = gameDetails.roundWithResults.map {
                        RoundEntity(
                            id = it.roundId,
                            bet = it.bet,
                            gameId = gameId
                        )
                    }
                    val results =
                        gameDetails.roundWithResults.map { gameRounds -> gameRounds.results.map { result -> result.toEntity() } }
                            .flatten()
                    roundDao.insertRounds(rounds)
                    resultDao.insertResults(results)
                }
        }
    }

    override fun disconnectFromGameUpdates() {
        gameUpdatesJob?.cancel()
    }

}