package org.hank.botm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.hank.botm.Config
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.data.sync.SyncManager
import org.hank.botm.ui.Lobby
import org.hank.botm.ui.GameRoom
import org.hank.botm.ui.Screen
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class SplashViewModel(
    private val gameRepository: GameRepository,
    private val syncManager: SyncManager,
): ViewModel() {

    private val _startDestination = MutableStateFlow<Screen?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val timeSource = TimeSource.Monotonic
            val mark = timeSource.markNow()
            val minSplashTime = 1.seconds

            // Perform sync if eligible for online (with 5-second timeout)
            if (Config.isEligibleForOnline) {
                withTimeoutOrNull(5.seconds) {
                    syncManager.performSync()
                }
            }

            val destination = gameRepository.game.firstOrNull()?.let {
                GameRoom(it.id)
            } ?: run {
                Lobby
            }

            // At least stay at Splash for 1 second
            if (mark.elapsedNow() < minSplashTime) {
                delay(minSplashTime - mark.elapsedNow())
            }

            _startDestination.value = destination
        }
    }
}