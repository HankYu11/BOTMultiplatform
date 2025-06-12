package org.hank.botm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.ui.Game
import org.hank.botm.ui.Screen
import org.hank.botm.ui.Setup

class AppViewModel(
    gameRepository: GameRepository,
): ViewModel() {
    private val _startDestination = MutableStateFlow<Screen?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        // directly nav to gamePage if has unfinished game
        gameRepository.game
            .onEach {
                if (it != null) {
                    _startDestination.value = Game(it.id)
                } else {
                    _startDestination.value = Setup
                }
            }.launchIn(viewModelScope)
    }
}