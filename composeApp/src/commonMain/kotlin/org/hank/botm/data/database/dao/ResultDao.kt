package org.hank.botm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.hank.botm.data.database.model.ResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {

    @Query("SELECT * FROM result")
    fun getAllResults(): Flow<List<ResultEntity>>

    @Query("SELECT * FROM result WHERE roundId = :roundId")
    fun getRoundResults(roundId: Long): Flow<List<ResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(result: List<ResultEntity>): List<Long>
}