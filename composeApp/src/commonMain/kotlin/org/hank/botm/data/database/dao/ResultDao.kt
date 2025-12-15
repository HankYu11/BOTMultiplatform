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

    @Query("SELECT * FROM result WHERE roundId = :roundId")
    suspend fun getResultsByRoundId(roundId: Int): List<ResultEntity>
}