package org.hank.botm.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.domain.usecase.CreateRoundUseCase
import org.hank.botm.ui.GameRoom
import org.hank.botm.ui.model.PlayerResult
import org.hank.botm.ui.state.HomeState

class GameRoomViewModel(
    savedStateHandle: SavedStateHandle,
    private val gameRepository: GameRepository,
    private val createRoundUseCase: CreateRoundUseCase,
) : ViewModel() {
    private val gameId = savedStateHandle.toRoute<GameRoom>().gameId

    // UI State
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // Navigation events
    private val _navigateToSetup = Channel<Unit>()
    val navigateToSetup = _navigateToSetup.receiveAsFlow()

    init {
        gameRepository.gameWithDetails
            .onEach {
                _state.value = _state.value.copy(gameWithDetails = it)
            }.launchIn(viewModelScope)

        refreshGame()
    }

    fun refreshGame() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, gameError = null)
            gameRepository.refreshGame(gameId).onFailure {
                _state.value = _state.value.copy(
                    isLoading = false, 
                    gameError = GameError.RefreshGameFailed(it.message)
                )
            }.onSuccess {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun submitResults(playerResults: List<PlayerResult>) {
        if (playerResults.size != 4) {
            _state.value = _state.value.copy(
                gameError = GameError.SummitResultError("The gameResult list size should be 4 but was ${playerResults.size}")
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, gameError = null)
            createRoundUseCase.invoke(gameId, state.value.bet, playerResults).onFailure {
                _state.value = _state.value.copy(
                    isLoading = false, 
                    gameError = GameError.CreateRoundFailed(it.message)
                )
            }.onSuccess {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun hideError() {
        _state.value = _state.value.copy(gameError = null)
    }

    fun updateBet(bet: Int) {
        _state.value = _state.value.copy(bet = bet)
    }

    fun startNewGame() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            gameRepository.clearGame()
            _state.value = _state.value.copy(isLoading = false)
            _navigateToSetup.send(Unit)
        }
    }
}

sealed interface GameError {
    val message: String?
    data class RefreshGameFailed(override val message: String?) : GameError
    data class CreateRoundFailed(override val message: String?) : GameError
    data class SummitResultError(override val message: String?) : GameError
}
