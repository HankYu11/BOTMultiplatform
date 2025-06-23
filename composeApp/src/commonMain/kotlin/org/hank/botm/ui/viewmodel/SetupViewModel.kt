package org.hank.botm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.domain.model.CreateGame
import org.hank.botm.domain.model.isValid
import org.hank.botm.domain.model.withNameChanged
import org.hank.botm.ui.state.SetupState

class SetupViewModel(
    private val gameRepository: GameRepository,
) : ViewModel() {

    // UI State
    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state.asStateFlow()

    // Navigation events
    private val _navigateToGame = Channel<Int>()
    val navigateToGame = _navigateToGame.receiveAsFlow()

    init {
        viewModelScope.launch {
            gameRepository.game.collectLatest { game ->
                _state.value = _state.value.copy(gameId = game?.id)
                game?.id?.let { gameId ->
                    _navigateToGame.send(gameId)
                }
            }
        }
    }

    fun updatePlayerName(index: Int, name: String) {
        _state.value = _state.value.copy(
            createGame = _state.value.createGame.withNameChanged(index, name)
        )
    }

    fun createGame(createGame: CreateGame) {
        if (createGame.isValid()) {
            viewModelScope.launch {
                _state.value = _state.value.copy(isLoading = true, error = null)
                gameRepository.createGame(createGame).let { result ->
                    result.fold(
                        onSuccess = {
                            _state.value = _state.value.copy(isLoading = false)
                            // Navigation will happen via the game flow in init
                        }, onFailure = {
                            _state.value = _state.value.copy(
                                isLoading = false, 
                                error = it.message ?: "Unknown error"
                            )
                        }
                    )
                }
            }
        } else {
            _state.value = _state.value.copy(error = "Please enter name for all players")
        }
    }

    fun dismissAlert() {
        _state.value = _state.value.copy(error = null)
    }
}
