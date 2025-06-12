package org.hank.botm.domain.model

import org.hank.botm.data.database.model.ResultEntity
import org.hank.botm.data.network.model.PlayerResultDto

data class Result(
    val id: Long = 0,
    val roundId: Long = 0,
    val playerId: Long,
    val profit: Int = 0,
)

fun Result.asEntity(roundId: Long) = ResultEntity(
    roundId = roundId,
    playerId = playerId,
    profit = profit
)

fun Result.asPlayerResultDto() = PlayerResultDto(
    // FIXME("Change app's id to Int")
    playerId = playerId.toInt(),
    profit = profit
)
