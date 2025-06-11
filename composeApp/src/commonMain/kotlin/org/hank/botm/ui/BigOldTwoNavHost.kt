package org.hank.botm.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.hank.botm.ui.screen.HomeScreen
import org.hank.botm.ui.screen.SetupScreen
import kotlinx.serialization.Serializable

sealed interface Screen

@Serializable
data object Setup: Screen

@Serializable
data class Game(
    val gameId: Long
): Screen

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
                    navController.navigate(route = Game(it)) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable<Game> {
            HomeScreen(
                navToSetup = {
                    navController.navigate(route = Setup) {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
    }
}