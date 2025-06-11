package org.hank.botm.ui.component

import androidx.compose.animation.core.copy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.hank.botm.domain.model.Player
import org.hank.botm.ui.model.PlayerResult
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameWinDialog(
    players: List<Player>,
    bet: Int,
    updateBet: (Int) -> Unit,
    submitRound: (List<PlayerResult>) -> Unit,
    dismissDialog: () -> Unit,
) {
    var playerResults by remember {
        mutableStateOf(
            players.map { player ->
                PlayerResult(
                    playerName = player.name,
                    playerId = player.id,
                )
            }
        )
    }

    var isError by remember {
        mutableStateOf(false)
    }

    BasicAlertDialog(
        onDismissRequest = { dismissDialog() },
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextField(
                    value = bet.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { newBet ->
                            isError = false
                            updateBet(newBet)
                        } ?: run {
                            isError = true
                        }
                    },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Please enter a number")
                        }
                    },
                    label = { Text("bet") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(playerResults.size) { index ->
                        val playerResult = playerResults[index]
                        TextField(
                            value = playerResult.cardsInHand,
                            onValueChange = { newValue ->
                                playerResults = playerResults.mapIndexed { idx, playerResult ->
                                    if (idx == index) {
                                        playerResult.copy(cardsInHand = newValue)
                                    } else {
                                        playerResult
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text(playerResult.playerName) },
                            enabled = !playerResult.isWinner,
                            trailingIcon = {
                                RadioButton(
                                    selected = playerResult.isWinner,
                                    onClick = {
                                        playerResults = playerResults.map {
                                            if (it.playerId == playerResult.playerId) {
                                                it.copy(isWinner = true, cardsInHand = "")
                                            } else {
                                                it.copy(isWinner = false)
                                            }
                                        }
                                    },
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.size(20.dp))

                Button(
                    onClick = {
                        submitRound(playerResults)
                    }, enabled = playerResults.hasValidWinnersAndLosers()
                ) {
                    Text(text = "Confirm")
                }
            }
        }
    }
}

private fun List<PlayerResult>.hasValidWinnersAndLosers(): Boolean {
    return count { it.isWinner } == 1 && filterNot { it.isWinner }.all { playerResult ->
        playerResult.cardsInHand.toIntOrNull()?.let { it > 0 } ?: false
    }
}

@Preview
@Composable
fun GameWinDialogPreview() {
    val players = listOf(
        Player(gameId = 0L, balance = 10, name = "Hank"),
        Player(gameId = 0L, balance = 10, name = "Steven"),
        Player(gameId = 0L, balance = 10, name = "Hanna"),
        Player(gameId = 0L, balance = 10, name = "Cindy"),
    )
    GameWinDialog(
        players = players,
        bet = 1,
        updateBet = {},
        submitRound = {},
        dismissDialog = { },
    )
}