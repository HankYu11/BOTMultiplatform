package org.hank.botm.domain.model

import org.hank.botm.data.network.model.CreateGameDto

data class CreateGame(
    val player1Name: String = "",
    val player2Name: String = "",
    val player3Name: String = "",
    val player4Name: String = "",
)

fun CreateGame.isValid() = player1Name.isNotEmpty() && player2Name.isNotEmpty() && player3Name.isNotEmpty() && player4Name.isNotEmpty()

fun CreateGame.getNameByIndex(index: Int): String {
    return when (index) {
        0 -> player1Name
        1 -> player2Name
        2 -> player3Name
        3 -> player4Name
        else -> throw IllegalArgumentException("Index out of bounds")
    }
}

fun CreateGame.withNameChanged(index: Int, newName: String): CreateGame {
    return when (index) {
        0 -> copy(player1Name = newName)
        1 -> copy(player2Name = newName)
        2 -> copy(player3Name = newName)
        3 -> copy(player4Name = newName)
        else -> this
    }
}

fun CreateGame.toDto() = CreateGameDto(
    playerNames = listOf(player1Name, player2Name, player3Name, player4Name)
)