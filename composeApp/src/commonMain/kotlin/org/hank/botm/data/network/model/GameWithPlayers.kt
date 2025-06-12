package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class GameWithPlayers(
    val game: GameDto,
    val players: List<PlayerDto>,
)
