package org.hank.botm.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import botmultiplatform.composeapp.generated.resources.Res
import botmultiplatform.composeapp.generated.resources.bot
import org.hank.botm.ui.Lobby
import org.hank.botm.ui.Room
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

    LaunchedEffect(startDestination) {
        startDestination?.let { destination ->
            when (destination) {
                is Lobby -> navToLobby()
                is Room -> navToRoom(destination.gameId)
                else -> {
                    throw Exception("Unknown start destination: $destination")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            modifier = Modifier.size(100.dp)
                .align(Alignment.Center),
            painter = painterResource(Res.drawable.bot),
            contentDescription = "App icon",
        )
    }
}