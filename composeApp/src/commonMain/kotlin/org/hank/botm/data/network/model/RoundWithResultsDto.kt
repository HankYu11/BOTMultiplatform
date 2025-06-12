package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RoundWithResultsDto(
    val roundId: Int,
    val bet: Int?,
    val results: List<ResultDto>
)
