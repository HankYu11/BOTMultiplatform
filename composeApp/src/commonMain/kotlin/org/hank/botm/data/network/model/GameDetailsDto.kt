package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class GameDetailsDto(
    val game: GameDto,
    val players: List<PlayerDto>,
    val roundWithResults: List<RoundWithResultsDto>,
)