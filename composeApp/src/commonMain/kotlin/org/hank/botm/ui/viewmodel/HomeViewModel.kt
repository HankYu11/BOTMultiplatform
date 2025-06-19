package org.hank.botm.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.domain.usecase.CreateRoundUseCase
import org.hank.botm.ui.Game
import org.hank.botm.ui.model.PlayerResult

class HomeViewModel(
    savedStateHandle: SavedStateHandle,
    private val gameRepository: GameRepository,
    private val createRoundUseCase: CreateRoundUseCase,
) : ViewModel() {
    private val gameId = savedStateHandle.toRoute<Game>().gameId

    val gameWithDetails = gameRepository.gameWithDetails

    private val _bet = MutableStateFlow(1)
    val bet = _bet.asStateFlow()

    private val _shouldNavToSetup = MutableStateFlow(false)
    val shouldNavToSetup = _shouldNavToSetup.asStateFlow()

    init {
        viewModelScope.launch {
            gameRepository.refreshGame(gameId).onFailure {
                // TODO("handle error")
            }
        }
    }

    fun submitResults(playerResults: List<PlayerResult>) {
        if (playerResults.size != 4) throw Exception("The gameResult list size should be 4 but was ${playerResults.size}")

        viewModelScope.launch {
            createRoundUseCase.invoke(gameId, bet.value, playerResults).onFailure {
                // TODO("handle error")
            }
        }
    }

    fun updateBet(bet: Int) {
        _bet.value = bet
    }

    fun startNewGame() {
        viewModelScope.launch {
            gameRepository.clearGame()
            _shouldNavToSetup.value = true
        }
    }

    fun finishNav() {
        _shouldNavToSetup.value = false
    }
}
