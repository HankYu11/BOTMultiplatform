package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable
import org.hank.botm.data.database.model.RoundEntity

@Serializable
data class RoundDto(
    val id: Int,
    val gameId: Int,
    val bet: Int,
)

fun RoundDto.toEntity() = RoundEntity(
    id = id,
    gameId = gameId,
    bet = bet,
)