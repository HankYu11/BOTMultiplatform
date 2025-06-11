package org.hank.botm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.data.repository.PlayerRepository
import org.hank.botm.domain.model.Game
import org.hank.botm.domain.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SetupViewModel (
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
) : ViewModel() {

    private val _alertMessage = MutableStateFlow("")
    val alertMessage = _alertMessage.asStateFlow()

    private val _gameId = MutableStateFlow(0L)
    val gameId = _gameId.asStateFlow()

    fun createGame(
        playerNames: List<String>
    ) {
        if (playerNames.any { it.isEmpty() }) {
            _alertMessage.value = "Please enter name for all players"
        } else {
            viewModelScope.launch {
                val gameId = gameRepository.insertGame(Game())
                playerNames.map { Player(name = it, gameId = gameId) }.forEach {
                    playerRepository.insertPlayer(it)
                }
                _gameId.value = gameId
            }
        }
    }

    fun finishNav() {
        _gameId.value = 0
    }

    fun dismissAlert() {
        _alertMessage.value = ""
    }
}