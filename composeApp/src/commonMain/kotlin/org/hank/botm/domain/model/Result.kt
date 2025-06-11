package org.hank.botm.domain.model

import org.hank.botm.data.database.model.ResultEntity

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
