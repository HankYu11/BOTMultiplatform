package org.hank.botm.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.hank.botm.ui.screen.HomeScreen
import org.hank.botm.ui.screen.SetupScreen
import kotlinx.serialization.Serializable
import org.hank.botm.ui.screen.GameLobbyScreen
import org.hank.botm.ui.screen.SplashScreen

sealed interface Screen

@Serializable
data object Splash: Screen

@Serializable
data object Setup: Screen

@Serializable
data class Room(
    val gameId: Int
): Screen

@Serializable
data object Lobby: Screen

@Composable
fun BigOldTwoNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
) {
    NavHost(navController = navController, startDestination = Splash) {
        composable<Splash> {
            SplashScreen(
                navToLobby = {
                    navController.navigate(route = Lobby) {
                        popUpTo(Splash) { inclusive = true }
                    }
                },
                navToRoom = {
                    navController.navigate(route = Room(it)) {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            )
        }

        composable<Setup> {
            SetupScreen(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                navToGame = {
                    navController.navigate(route = Room(it)) {
                        popUpTo<Setup>() { inclusive = true }
                    }
                }
            )
        }
        composable<Room> {
            HomeScreen(
                navToLobby = {
                    navController.navigate(route = Lobby) {
                        popUpTo<Room> { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
        composable<Lobby> {
            GameLobbyScreen(
                navToSetup = {
                    navController.navigate(route = Setup) {
                        launchSingleTop = true
                    }
                },
                navToGame = {
                    navController.navigate(route = Room(it)) {
                        popUpTo(Lobby) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
    }
}