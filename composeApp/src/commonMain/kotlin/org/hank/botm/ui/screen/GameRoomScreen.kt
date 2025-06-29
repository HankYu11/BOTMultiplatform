package org.hank.botm.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.hank.botm.domain.model.*
import org.hank.botm.ui.component.GameWinDialog
import org.hank.botm.ui.component.ResultView
import org.hank.botm.ui.model.PlayerResult
import org.hank.botm.ui.theme.BigOldTwoTheme
import org.hank.botm.ui.viewmodel.GameError
import org.hank.botm.ui.viewmodel.GameRoomViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    navToLobby: () -> Unit,
    modifier: Modifier = Modifier,
    gameRoomViewModel: GameRoomViewModel = koinViewModel(),
) {
    val state by gameRoomViewModel.state.collectAsState()

    // Handle navigation
    LaunchedEffect(Unit) {
        gameRoomViewModel.navigateToSetup.collect {
            navToLobby()
        }
    }

    // Show error dialog if there's an error
    state.gameError?.let {
        when (it) {
            is GameError.CreateRoundFailed -> {
                AlertDialog(
                    onDismissRequest = { gameRoomViewModel.hideError() },
                    title = { Text(text = "Failed to create a game") },
                    text = { Text(text = it.message ?: "Something went wrong") },
                    confirmButton = {
                        TextButton(onClick = { gameRoomViewModel.hideError() }) {
                            Text("Ok")
                        }
                    }
                )
            }

            is GameError.RefreshGameFailed -> {
                AlertDialog(
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                    onDismissRequest = { /* shouldn't triggered */ },
                    title = { Text(text = "Something went wrong") },
                    text = { Text(text = it.message ?: "Something went wrong") },
                    confirmButton = {
                        TextButton(onClick = { gameRoomViewModel.refreshGame() }) {
                            Text("Retry")
                        }
                    }
                )
            }

            is GameError.SummitResultError -> {
                AlertDialog(
                    onDismissRequest = { gameRoomViewModel.hideError() },
                    title = { Text(text = "Summit Results Error") },
                    text = { Text(text = it.message ?: "Something went wrong") },
                    confirmButton = {
                        TextButton(onClick = { gameRoomViewModel.hideError() }) {
                            Text("Ok")
                        }
                    }
                )
            }
        }
    }

    state.gameWithDetails?.let {
        HomeScreen(
            bet = state.bet,
            gameWithDetails = it,
            startNewGame = { gameRoomViewModel.startNewGame() },
            updateBet = { newBet ->
                gameRoomViewModel.updateBet(newBet)
            },
            updateGame = { gameResults ->
                gameRoomViewModel.submitResults(gameResults)
            },
            modifier = modifier,
        )
    }

    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun HomeScreen(
    bet: Int,
    gameWithDetails: GameWithDetails,
    startNewGame: () -> Unit,
    updateBet: (Int) -> Unit,
    updateGame: (List<PlayerResult>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Spacer(modifier = Modifier.size(1.dp))

            Text(
                text = "Game ID: ${gameWithDetails.gameId}",
            )

            Text(
                text = "Bet: $bet",
            )
        }
        ResultView(
            modifier = Modifier.weight(1f),
            players = gameWithDetails.players,
            roundsWithResults = gameWithDetails.roundsWithResults
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                showDialog = true
            }) {
                Text(text = "Win!")
            }
            Button(onClick = {
                startNewGame()
            }) {
                Text(text = "New Game")
            }
        }

        if (showDialog) {
            GameWinDialog(
                players = gameWithDetails.players,
                bet = bet,
                updateBet = updateBet,
                submitRound = { gameResults ->
                    updateGame(gameResults)
                    showDialog = false
                },
                dismissDialog = { showDialog = false }
            )
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    val players = listOf(
        Player(
            id = 0,
            gameId = 0,
            name = "Hank",
            balance = 30,
        ),
        Player(
            id = 1,
            gameId = 0,
            name = "Bob",
            balance = -10,
        ),
        Player(
            id = 2,
            gameId = 0,
            name = "Ray",
            balance = -10,
        ),
        Player(
            id = 3,
            gameId = 0,
            name = "Jim",
            balance = -10,
        ),
    )
    val results = listOf(
        Result(
            playerId = 0,
            profit = 15
        ),
        Result(
            playerId = 1,
            profit = -5,
        ),
        Result(
            playerId = 2,
            profit = -5,
        ),
        Result(
            playerId = 3,
            profit = -5,
        ),
    )

    val roundsWithResults = listOf(
        RoundWithResults(Round(id = 0, gameId = 0, bet = 1), results)
    )

    val gameWithDetails = GameWithDetails(
        gameId = 0,
        players = players,
        roundsWithResults = roundsWithResults
    )

    BigOldTwoTheme {
        HomeScreen(
            bet = 1,
            gameWithDetails = gameWithDetails,
            {},
            {},
            { _ -> },
            Modifier.fillMaxSize()
        )
    }
}
