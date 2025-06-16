package org.hank.botm.data.database.model

import androidx.room.Embedded
import androidx.room.Relation
import org.hank.botm.domain.model.RoundWithResults

data class RoundWithResultsEntity(
    @Embedded val round: RoundEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "roundId"
    )
    val results: List<ResultEntity>
)

fun RoundWithResultsEntity.asDomain() = RoundWithResults(
    round = round.asDomain(),
    results = results.map { it.asDomain() }
)