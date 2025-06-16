package org.hank.botm.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import org.hank.botm.data.database.AppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao
import org.hank.botm.data.database.model.RoundEntity
import org.hank.botm.data.database.model.asDomain
import org.hank.botm.data.network.api.GameApi
import org.hank.botm.data.network.model.toEntity
import org.hank.botm.domain.model.CreateGame
import org.hank.botm.domain.model.Game
import org.hank.botm.domain.model.GameWithDetails
import org.hank.botm.domain.model.toDto

interface GameRepository {
    val game: Flow<Game?>
    val gameWithDetails: Flow<GameWithDetails>
    suspend fun createGame(createGame: CreateGame)
    suspend fun clearGame()
    suspend fun refreshGame(id: Int)
}

class GameRepositoryImpl(
    private val appDatabase: AppDatabase,
    private val gameDao: GameDao,
    private val playerDao: PlayerDao,
    private val roundDao: RoundDao,
    private val resultDao: ResultDao,
    private val gameApi: GameApi,
    private val ioDispatcher: CoroutineDispatcher,
) : GameRepository {
    override val game: Flow<Game?> = gameDao.getNewestGame()
        .map { it?.asDomain() }

    override val gameWithDetails: Flow<GameWithDetails> =
        gameDao.getNewestGameWithDetails()
            .map { it.asDomain() }
            .map { gameWithDetails ->
                // reverse rounds to show newest first
                gameWithDetails.copy(
                    roundsWithResults = gameWithDetails.roundsWithResults.reversed()
                )
            }

    override suspend fun createGame(createGame: CreateGame) {
        withContext(ioDispatcher) {
            val gameWithPlayers = gameApi.createGame(createGame.toDto())
            appDatabase.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    gameDao.insertGame(gameWithPlayers.game.toEntity())
                    playerDao.insertPlayers(gameWithPlayers.players.map { it.toEntity() })
                }
            }
        }
    }

    override suspend fun clearGame() {
        withContext(ioDispatcher) {
            gameDao.clearGame()
        }
    }

    override suspend fun refreshGame(id: Int) {
        val gameDetails = gameApi.getGame(id)
        playerDao.insertPlayers(gameDetails.players.map { it.toEntity() })
        val rounds = gameDetails.roundWithResults.map { RoundEntity(id = it.roundId, bet = it.bet, gameId = id) }
        val results = gameDetails.roundWithResults.map { gameRounds -> gameRounds.results.map { result -> result.toEntity() } }.flatten()
        roundDao.insertRounds(rounds)
        resultDao.insertResults(results)
    }
}