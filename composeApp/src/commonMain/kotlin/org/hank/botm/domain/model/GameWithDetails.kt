package org.hank.botm.domain.model

data class GameWithDetails(
    val gameId: Int,
    val players: List<Player>,
    val roundsWithResults: List<RoundWithResults>
)
