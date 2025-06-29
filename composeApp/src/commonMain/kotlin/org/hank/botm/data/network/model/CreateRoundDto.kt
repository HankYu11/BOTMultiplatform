package org.hank.botm.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateRoundDto(
    val gameId: Int,
    val bet: Int,
    @SerialName("results")
    val playerResults: List<PlayerResultDto>
)
