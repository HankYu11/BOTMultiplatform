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
    val state by setupViewModel.state.collectAsState()

    // Handle navigation
    LaunchedEffect(Unit) {
        setupViewModel.navigateToGame.collect { gameId ->
            navToGame(gameId)
        }
    }

    // Show error dialog if there's an error
    state.error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { setupViewModel.dismissAlert() },
            title = { Text(text = "Alert") },
            text = { Text(text = errorMessage) },
            confirmButton = {
                TextButton(onClick = { setupViewModel.dismissAlert() }) { 
                    Text("Confirm") 
                }
            },
        )
    }

    // Show loading indicator if loading
    if (state.isLoading) {
        // You can add a loading indicator here
    }

    SetupScreen(
        createGame = { createGame ->
            setupViewModel.createGame(createGame)
        },
        createGameState = state.createGame,
        updatePlayerName = { index, name ->
            setupViewModel.updatePlayerName(index, name)
        },
        modifier = modifier,
    )
}

@Composable
fun SetupScreen(
    createGame: (CreateGame) -> Unit,
    createGameState: CreateGame,
    updatePlayerName: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    name = createGameState.getNameByIndex(it),
                    onNameChange = { index, name ->
                        updatePlayerName(index, name)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.size(45.dp))

        Button(
            onClick = {
                createGame(createGameState)
            },
        ) {
            Text(text = "Start Game")
        }
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
        createGame = {},
        createGameState = CreateGame(),
        updatePlayerName = { _, _ -> },
        modifier = Modifier.fillMaxSize()
    )
}

@Preview
@Composable
fun PlayerSetupPreview() {
    PlayerSetupView(0, "Hank", { _, _ -> })
}
