package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable
import org.hank.botm.data.database.model.ResultEntity

@Serializable
data class ResultDto(
    val id: Int,
    val profit: Int,
    val roundId: Int,
    val playerId: Int
)

fun ResultDto.toEntity() = ResultEntity(
    id = id,
    profit = profit,
    roundId = roundId,
    playerId = playerId
)