package org.hank.botm.data.database.model

import androidx.room.Embedded
import androidx.room.Relation

data class RoundWithResults(
    @Embedded val round: RoundEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "roundId"
    )
    val results: List<ResultEntity>
)