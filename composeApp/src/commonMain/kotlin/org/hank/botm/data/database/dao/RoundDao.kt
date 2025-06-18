package org.hank.botm.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.hank.botm.data.database.model.PlayerEntity
import org.hank.botm.data.database.model.ResultEntity
import org.hank.botm.data.database.model.RoundEntity
import org.hank.botm.data.database.model.RoundWithResultsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: RoundEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRounds(rounds: List<RoundEntity>)
}