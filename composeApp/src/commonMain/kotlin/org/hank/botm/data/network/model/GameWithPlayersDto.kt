package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class GameWithPlayersDto(
    val game: GameDto,
    val players: List<PlayerDto>,
)
