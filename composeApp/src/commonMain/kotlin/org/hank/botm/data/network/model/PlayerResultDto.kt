package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerResultDto(
    val playerId: Int,
    val profit: Int,
)
