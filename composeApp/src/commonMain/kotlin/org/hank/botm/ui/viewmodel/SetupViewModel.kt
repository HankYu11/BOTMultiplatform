package org.hank.botm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.data.repository.PlayerRepository
import org.hank.botm.domain.model.Game
import org.hank.botm.domain.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.hank.botm.data.network.api.GameApi
import org.hank.botm.data.network.model.CreateGameDto
import org.hank.botm.domain.model.CreateGame
import org.hank.botm.domain.model.isValid

class SetupViewModel(
    private val gameRepository: GameRepository,
) : ViewModel() {

    private val _alertMessage = MutableStateFlow("")
    val alertMessage = _alertMessage.asStateFlow()

    val gameId = gameRepository.game.map { it?.id }

    fun createGame(
        createGame: CreateGame
    ) {
        if (createGame.isValid()) {
            viewModelScope.launch {
                gameRepository.createGame(createGame)
            }
        } else {
            _alertMessage.value = "Please enter name for all players"
        }
    }

    fun dismissAlert() {
        _alertMessage.value = ""
    }
}