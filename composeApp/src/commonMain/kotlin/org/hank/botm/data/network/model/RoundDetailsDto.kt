package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class RoundDetailsDto(
    val round: RoundDto,
    val players: List<PlayerDto>,
    val results: List<ResultDto>
)