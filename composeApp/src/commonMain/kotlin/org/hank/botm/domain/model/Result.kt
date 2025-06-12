package org.hank.botm.domain.model

import org.hank.botm.data.database.model.ResultEntity
import org.hank.botm.data.network.model.PlayerResultDto

data class Result(
    val id: Int = 0,
    val roundId: Int = 0,
    val playerId: Int,
    val profit: Int = 0,
)

fun Result.asEntity(roundId: Int) = ResultEntity(
    roundId = roundId,
    playerId = playerId,
    profit = profit
)

fun Result.asPlayerResultDto() = PlayerResultDto(
    playerId = playerId,
    profit = profit
)
