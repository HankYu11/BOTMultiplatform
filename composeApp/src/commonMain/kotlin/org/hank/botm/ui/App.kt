package org.hank.botm.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.navigation.compose.rememberNavController
import org.hank.botm.Config
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
                CenterAlignedTopAppBar(
                    title = {
                        Column {
                            Text("大老二計分板")
                            if (Config.offlineMode) {
                                Text(
                                    text = "Offline Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
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