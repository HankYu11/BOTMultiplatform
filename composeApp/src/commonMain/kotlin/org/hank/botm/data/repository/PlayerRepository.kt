package org.hank.botm.data.repository

import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.model.asDomain
import org.hank.botm.domain.model.Player
import org.hank.botm.domain.model.asEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface PlayerRepository {
    fun getPlayers(gameId: Int): Flow<List<Player>>
    fun getCurrentGamePlayers(): Flow<List<Player>>

    suspend fun insertPlayer(player: Player)
    suspend fun getPlayer(id: Int): Player
    suspend fun updatePlayerBalance(id: Int, balance: Int)
}

class PlayerRepositoryImpl constructor(
    private val playerDao: PlayerDao,
    private val ioDispatcher: CoroutineDispatcher
) : PlayerRepository {
    override fun getPlayers(gameId: Int): Flow<List<Player>> {
        return playerDao.getPlayersByGame(gameId).map {
            it.map { it.asDomain() }
        }
    }

    override fun getCurrentGamePlayers(): Flow<List<Player>> {
        return playerDao.getCurrentGamePlayers().map {
            it.map { it.asDomain() }
        }
    }

    override suspend fun insertPlayer(player: Player) = withContext(ioDispatcher) {
        playerDao.insertPlayer(player.asEntity())
    }

    override suspend fun getPlayer(id: Int): Player = withContext(ioDispatcher) {
        playerDao.getPlayer(id).asDomain()
    }

    override suspend fun updatePlayerBalance(id: Int, profit: Int) {
        playerDao.updatePlayerBalance(id, profit)
    }
}