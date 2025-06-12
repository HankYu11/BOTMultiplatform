package org.hank.botm.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.model.asDomain
import org.hank.botm.data.network.api.GameApi
import org.hank.botm.data.network.model.toEntity
import org.hank.botm.domain.model.CreateGame
import org.hank.botm.domain.model.Game
import org.hank.botm.domain.model.toDto

interface GameRepository {
    val game: Flow<Game?>
    suspend fun createGame(createGame: CreateGame)
}

class GameRepositoryImpl (
    private val gameDao: GameDao,
    private val playerDao: PlayerDao,
    private val gameApi: GameApi,
    private val ioDispatcher: CoroutineDispatcher,
) : GameRepository {
    override val game: Flow<Game?> = gameDao.getNewestGame()
        .map { it?.asDomain() }

    override suspend fun createGame(createGame: CreateGame) {
        withContext(ioDispatcher) {
            val gameWithPlayers = gameApi.createGame(createGame.toDto())
            gameDao.insertGame(gameWithPlayers.game.toEntity())
            playerDao.insertPlayers(gameWithPlayers.players.map { it.toEntity() })
        }
    }
}