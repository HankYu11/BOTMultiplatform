package org.hank.botm.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import org.hank.botm.domain.model.GameWithDetails
import org.hank.botm.domain.model.Player
import org.hank.botm.domain.model.Result
import org.hank.botm.domain.model.Round
import org.hank.botm.domain.model.RoundWithResults
import org.hank.botm.ui.component.GameWinDialog
import org.hank.botm.ui.model.PlayerResult
import org.hank.botm.ui.theme.BigOldTwoTheme
import org.hank.botm.ui.viewmodel.GameError
import org.hank.botm.ui.viewmodel.HomeViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    navToSetup: () -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = koinViewModel(),
) {
    val state by homeViewModel.state.collectAsState()

    // Handle navigation
    LaunchedEffect(Unit) {
        homeViewModel.navigateToSetup.collect {
            navToSetup()
        }
    }

    // Show error dialog if there's an error
    state.gameError?.let {
        when (it) {
            is GameError.CreateRoundFailed -> {
                AlertDialog(
                    onDismissRequest = { homeViewModel.hideError() },
                    title = { Text(text = "Failed to create a game") },
                    text = { Text(text = it.message ?: "Something went wrong" ) },
                    confirmButton = {
                        TextButton(onClick = { homeViewModel.hideError() }) {
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
                        TextButton(onClick = { homeViewModel.refreshGame() }) {
                            Text("Retry")
                        }
                    }
                )
            }
        }
    }

    // Show generic error if there's one
    state.error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { homeViewModel.hideError() },
            title = { Text(text = "Error") },
            text = { Text(text = errorMessage) },
            confirmButton = {
                TextButton(onClick = { homeViewModel.hideError() }) {
                    Text("Ok")
                }
            }
        )
    }

    // Show loading indicator if loading
    if (state.isLoading) {
        // You can add a loading indicator here
    }

    state.gameWithDetails?.let {
        HomeScreen(
            bet = state.bet,
            gameWithDetails = it,
            startNewGame = { homeViewModel.startNewGame() },
            updateBet = { newBet ->
                homeViewModel.updateBet(newBet)
            },
            updateGame = { gameResults ->
                homeViewModel.submitResults(gameResults)
            },
            modifier = modifier,
        )
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
