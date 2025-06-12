package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class ResultDto(
    val id: Int,
    val profit: Int?,
    val roundId: Int,
    val playerId: Int
)