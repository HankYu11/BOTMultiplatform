package org.hank.botm.ui.model

data class PlayerResult(
    val playerName: String,
    val playerId: Long,
    val cardsInHand: String = "",
    val isWinner: Boolean = false,
)
