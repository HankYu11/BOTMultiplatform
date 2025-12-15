package org.hank.botm.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import botmultiplatform.composeapp.generated.resources.Res
import botmultiplatform.composeapp.generated.resources.bot
import org.hank.botm.Config
import org.hank.botm.data.sync.SyncState
import org.hank.botm.ui.Lobby
import org.hank.botm.ui.GameRoom
import org.hank.botm.ui.viewmodel.SplashViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    navToLobby: () -> Unit,
    navToRoom: (gameId: Int) -> Unit,
    splashViewModel: SplashViewModel = koinViewModel(),
) {
    val startDestination by splashViewModel.startDestination.collectAsState()
    val syncState by splashViewModel.syncState.collectAsState()

    LaunchedEffect(startDestination) {
        startDestination?.let { destination ->
            when (destination) {
                is Lobby -> navToLobby()
                is GameRoom -> navToRoom(destination.gameId)
                else -> {
                    throw Exception("Unknown start destination: $destination")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier.size(100.dp),
                painter = painterResource(Res.drawable.bot),
                contentDescription = "App icon",
            )

            if (Config.isEligibleForOnline) {
                Spacer(modifier = Modifier.height(24.dp))

                when (syncState) {
                    is SyncState.Syncing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Syncing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is SyncState.Error -> {
                        Text(
                            text = "Offline mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}