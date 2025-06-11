package org.hank.botm.data.repository

import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.model.asDomain
import org.hank.botm.domain.model.Game
import org.hank.botm.domain.model.asEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface GameRepository {
    suspend fun fetchExistingGame(): Game?
    suspend fun fetchGame(gameId: Long): Game
    suspend fun insertGame(game: Game): Long
    suspend fun finishGame(gameId: Long)
}

class GameRepositoryImpl (
    private val gameDao: GameDao,
    private val ioDispatcher: CoroutineDispatcher
) : GameRepository {
    override suspend fun fetchExistingGame(): Game? = withContext(ioDispatcher) {
        gameDao.fetchUnFinishedGame()?.asDomain()
    }

    override suspend fun fetchGame(gameId: Long): Game = withContext(ioDispatcher) {
        gameDao.fetchGame(gameId).asDomain()
    }

    override suspend fun insertGame(game: Game) = withContext(ioDispatcher) {
        gameDao.insertGame(game.asEntity())
    }

    override suspend fun finishGame(gameId: Long) = withContext(ioDispatcher) {
        gameDao.finishedGame(gameId = gameId)
    }

}