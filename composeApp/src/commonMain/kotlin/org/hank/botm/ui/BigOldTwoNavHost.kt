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

sealed interface Screen

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
    startDestination: Screen,
    innerPadding: PaddingValues,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Setup> {
            SetupScreen(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                navToGame = {
                    navController.navigate(route = Room(it)) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable<Room> {
            HomeScreen(
                navToSetup = {
                    navController.navigate(route = Setup) {
                        launchSingleTop = true
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
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}