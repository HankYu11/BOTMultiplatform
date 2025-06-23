package org.hank.botm.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.hank.botm.ui.viewmodel.LobbyViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLobbyScreen(
    navToSetup: () -> Unit,
    navToGame: (gameId: Int) -> Unit,
    modifier: Modifier = Modifier,
    lobbyViewModel: LobbyViewModel = koinViewModel(),
) {
    val state by lobbyViewModel.state.collectAsState()

    var gameId by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        lobbyViewModel.navigateToRoom.collect { gameId ->
            navToGame(gameId)
        }
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { lobbyViewModel.dismissError() },
            title = { Text(text = "Failed to join the game") },
            text = { Text(text = "Please check the game ID and try again.") },
            confirmButton = {
                TextButton(onClick = { lobbyViewModel.dismissError() }) {
                    Text("Ok")
                }
            }
        )
    }

    if (state.showJoinGameDialog) {
        JoinRoomDialog(
            gameId = gameId,
            onGameIdChange = { gameId = it },
            onDismissRequest = lobbyViewModel::hideJoinGameDialog,
            onJoinClicked = lobbyViewModel::joinGame,
        )
    }

    Box(
        modifier = modifier,
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
                lobbyViewModel.showJoinGameDialog()
            }) {
                Text(text = "Join a Game")
            }

            Spacer(modifier = Modifier.size(20.dp))

            Button(onClick = {
                navToSetup()
            }) {
                Text(text = "Create a Game")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinRoomDialog(
    gameId: String,
    onGameIdChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onJoinClicked: (gameId: Int) -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = {
            onDismissRequest()
        },
        content = {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OutlinedTextField(
                        value = gameId,
                        onValueChange = {
                            onGameIdChange(it)
                        },
                        label = { Text("Game ID:") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.size(20.dp))

                    Button(
                        onClick = {
                            onJoinClicked(gameId.toInt())
                        },
                        enabled = gameId.toIntOrNull() != null,
                    ) {
                        Text(text = "Join Game")
                    }
                }
            }
        },
    )
}

@Preview
@Composable
fun JoinRoomDialogPreview() {
    JoinRoomDialog(
        "122",
        {},
        {},
        {},
    )
}