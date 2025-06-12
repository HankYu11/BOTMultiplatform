package org.hank.botm.data.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.hank.botm.domain.model.Result

@Entity(
    tableName = "result",
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
data class ResultEntity(
    @PrimaryKey
    val id: Int = 0,
    val roundId: Int,
    val playerId: Int,
    val profit: Int,
)

fun ResultEntity.asDomain() = Result(
    id = id,
    playerId = playerId,
    profit = profit,
    roundId = roundId,
)