package org.hank.botm.domain.model

import org.hank.botm.data.network.model.CreateGameDto

data class CreateGame(
    val playerOne: String,
    val playerTwo: String,
    val playerThree: String,
    val playerFour: String,
)

fun CreateGame.toDto() = CreateGameDto(
    playerNames = listOf(playerOne, playerTwo, playerThree, playerFour)
)