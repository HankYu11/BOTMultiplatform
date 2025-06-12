package org.hank.botm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.hank.botm.data.database.model.GameEntity

@Dao
interface GameDao {
    @Query("SELECT * FROM game ORDER BY id DESC LIMIT 1")
    fun getNewestGame(): Flow<GameEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(gameEntity: GameEntity)
}