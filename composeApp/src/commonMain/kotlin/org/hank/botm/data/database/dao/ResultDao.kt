package org.hank.botm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.hank.botm.data.database.model.ResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(result: List<ResultEntity>)

    // Sync-related queries
    @Query("SELECT * FROM result WHERE roundId = :roundId")
    suspend fun getResultsByRoundId(roundId: Int): List<ResultEntity>

    @Query("UPDATE result SET serverId = :serverId WHERE id = :resultId")
    suspend fun updateServerId(resultId: Int, serverId: Int)
}