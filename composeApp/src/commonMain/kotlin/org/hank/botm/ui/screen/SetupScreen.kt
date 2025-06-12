package org.hank.botm.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.hank.botm.domain.model.CreateGame
import org.hank.botm.domain.model.getNameByIndex
import org.hank.botm.domain.model.withNameChanged
import org.hank.botm.ui.viewmodel.SetupViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SetupScreen(
    navToGame: (gameId: Int) -> Unit,
    modifier: Modifier = Modifier,
    setupViewModel: SetupViewModel = koinViewModel(),
) {
    val gameId by setupViewModel.gameId.collectAsState(null)
    val alertMessage by setupViewModel.alertMessage.collectAsState()

    LaunchedEffect(gameId) {
        gameId?.let(navToGame)
    }

    SetupScreen(
        setupViewModel::createGame,
        alertMessage,
        setupViewModel::dismissAlert,
        modifier,
    )
}

@Composable
fun SetupScreen(
    createGame: (CreateGame) -> Unit,
    alertMessage: String,
    dismissAlert: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var players by remember {
        mutableStateOf(CreateGame())
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(4) {
                PlayerSetupView(
                    index = it,
                    name = players.getNameByIndex(it),
                    onNameChange = { index, name ->
                        players = players.withNameChanged(index, name)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.size(45.dp))

        Button(
            onClick = {
                createGame(players)
            },
        ) {
            Text(text = "Start Game")
        }
    }

    if (alertMessage.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = dismissAlert,
            title = { Text(text = "Alert") },
            text = { Text(text = alertMessage) },
            confirmButton = {
                TextButton(onClick = dismissAlert) { Text("Confirm") }
            },
        )
    }
}

@Composable
fun PlayerSetupView(
    index: Int,
    name: String,
    onNameChange: (index: Int, name: String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = "Player ${index + 1}")
            },
            value = name,
            onValueChange = {
                onNameChange(index, it)
            },
            trailingIcon = {
                if (name.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onNameChange(index, "")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun SetupScreenPreview() {
    SetupScreen(
        {},
        "",
        {},
        modifier = Modifier.fillMaxSize()
    )
}

@Preview
@Composable
fun PlayerSetupPreview() {
    PlayerSetupView(0, "Hank", { _, _ -> })
}
