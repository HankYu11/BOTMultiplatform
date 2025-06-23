package org.hank.botm.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import org.hank.botm.ui.theme.BigOldTwoTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.KoinApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    KoinApplication.init()

    val navController = rememberNavController()
    BigOldTwoTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text("大老二計分板")
                    }
                )
            }
        ) { innerPadding ->
            BigOldTwoNavHost(
                navController = navController,
                innerPadding = innerPadding
            )
        }
    }
}