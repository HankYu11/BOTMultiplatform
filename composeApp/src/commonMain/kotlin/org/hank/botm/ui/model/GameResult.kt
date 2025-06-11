package org.hank.botm.ui.model

import org.hank.botm.domain.model.Player

data class GameResult(
    val players: List<Player>,
    val roundResults: List<RoundResult>
)