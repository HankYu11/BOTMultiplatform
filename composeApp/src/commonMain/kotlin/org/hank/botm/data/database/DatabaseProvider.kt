package org.hank.botm.data.database

import org.hank.botm.data.database.dao.GameDao
import org.hank.botm.data.database.dao.PlayerDao
import org.hank.botm.data.database.dao.ResultDao
import org.hank.botm.data.database.dao.RoundDao

/**
 * A provider class that encapsulates database access objects.
 * This class reduces the number of dependencies that need to be injected into repositories.
 */
class DatabaseProvider(private val appDatabase: AppDatabase) {
    /**
     * Get the GameDao instance.
     */
    val gameDao: GameDao = appDatabase.getGameDao()
    
    /**
     * Get the PlayerDao instance.
     */
    val playerDao: PlayerDao = appDatabase.getPlayerDao()
    
    /**
     * Get the RoundDao instance.
     */
    val roundDao: RoundDao = appDatabase.getRoundDao()
    
    /**
     * Get the ResultDao instance.
     */
    val resultDao: ResultDao = appDatabase.getResultDao()
}