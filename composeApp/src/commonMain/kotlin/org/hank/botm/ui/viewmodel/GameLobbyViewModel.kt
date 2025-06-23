package org.hank.botm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.ui.state.LobbyState

class GameLobbyViewModel(
    private val gameRepository: GameRepository,
): ViewModel() {

    private val _state = MutableStateFlow(LobbyState())
    val state = _state.asStateFlow()

    private val _navigateToRoom = Channel<Int>()
    val navigateToRoom = _navigateToRoom.receiveAsFlow()

    fun showJoinGameDialog() {
        _state.value = _state.value.copy(showJoinGameDialog = true)
    }

    fun hideJoinGameDialog() {
        _state.value = _state.value.copy(showJoinGameDialog = false)
    }

    fun joinGame(gameId: Int) {
        viewModelScope.launch {
            gameRepository.checkGameExists(gameId).onSuccess {
                _navigateToRoom.send(gameId)
            }.onFailure {
                _state.value = _state.value.copy(error = "Game with id $gameId does not exist")
            }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }
}