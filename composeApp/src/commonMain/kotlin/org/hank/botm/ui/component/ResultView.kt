package org.hank.botm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.hank.botm.domain.model.Player
import org.hank.botm.domain.model.Result
import org.hank.botm.domain.model.Round
import org.hank.botm.domain.model.RoundWithResults
import org.hank.botm.ui.theme.BigOldTwoTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.absoluteValue

@Composable
fun ResultView(
    modifier: Modifier = Modifier,
    players: List<Player>,
    roundsWithResults: List<RoundWithResults>,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ResultTitle(
            modifier = Modifier.fillMaxWidth(),
            playerNames = players.map { it.name }
        )

        RoundView(
            modifier = Modifier.fillMaxWidth(),
            roundResults = players
                .sortedBy { it.id }
                .map { it.balance },
            isBalance = true
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(roundsWithResults) { roundResult ->
                RoundView(
                    modifier = Modifier.fillMaxWidth(),
                    roundResults = roundResult.results
                        .sortedBy { it.playerId }
                        .map { it.profit },
                    isBalance = false
                )
            }
        }
    }
}

@Composable
fun ResultTitle(
    playerNames: List<String>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        playerNames.forEach { player ->
            Box(
                modifier = Modifier
                    .size(84.dp, 48.dp)
                    .background(Color.LightGray),
            ) {
                Text(
                    text = player,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun RoundView(
    modifier: Modifier = Modifier,
    roundResults: List<Int>,
    isBalance: Boolean,
) {
    Row(
        modifier = modifier.then(
            if (isBalance) {
                Modifier
                    .background(Color.LightGray)
            } else {
                Modifier
            }
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        roundResults
            .forEach { result ->
                Box(
                    modifier = Modifier
                        .size(84.dp, 48.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = convertProfitToString(result),
                        color = if (result > 0) Color.Red else Color.Black
                    )
                }
            }
    }
}

private fun convertProfitToString(
    profit: Int
): String {
    return if (profit == 0) {
        "0"
    } else if (profit > 0) {
        "+ $profit"
    } else {
        "- ${profit.absoluteValue}"
    }
}

@Preview
@Composable
fun ResultScreenPreview() {
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

    BigOldTwoTheme {
        ResultView(
            modifier = Modifier.fillMaxSize(),
            players = players,
            roundsWithResults = roundsWithResults,
        )
    }
}