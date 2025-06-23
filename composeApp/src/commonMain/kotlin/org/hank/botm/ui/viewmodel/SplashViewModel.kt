package org.hank.botm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.ui.Lobby
import org.hank.botm.ui.Room
import org.hank.botm.ui.Screen
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class SplashViewModel(
    gameRepository: GameRepository,
): ViewModel() {

    private val _startDestination = MutableStateFlow<Screen?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val timeSource = TimeSource.Monotonic
            val mark = timeSource.markNow()
            val minSplashTime = 1.seconds

            val destination = gameRepository.game.firstOrNull()?.let {
                Room(it.id)
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