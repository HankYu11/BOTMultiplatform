package org.hank.botm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.hank.botm.data.repository.GameRepository

class AppViewModel(
    gameRepository: GameRepository,
): ViewModel() {
    private val _startDestination = MutableStateFlow<Screen?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        gameRepository.game
            .onEach {
                if (it != null) {
                    _startDestination.value = Room(it.id)
                } else {
                    _startDestination.value = Lobby
                }
            }.launchIn(viewModelScope)
    }
}