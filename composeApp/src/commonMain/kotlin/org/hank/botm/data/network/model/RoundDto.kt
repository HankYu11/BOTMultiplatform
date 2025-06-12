package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RoundDto(
    val id: Int,
    val gameId: Int,
    val bet: Int,
)
