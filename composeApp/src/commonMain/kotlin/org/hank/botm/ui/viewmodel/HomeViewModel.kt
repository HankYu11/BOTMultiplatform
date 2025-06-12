package org.hank.botm.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import org.hank.botm.data.repository.GameRepository
import org.hank.botm.domain.usecase.InsertResultsUseCase
import org.hank.botm.domain.model.Result
import org.hank.botm.domain.usecase.GetGameResultDataUseCase
import org.hank.botm.ui.model.PlayerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hank.botm.data.network.api.GameApi
import org.hank.botm.data.network.api.RoundApi
import org.hank.botm.ui.Game

class HomeViewModel(
    savedStateHandle: SavedStateHandle,
    private val insertResultsUseCase: InsertResultsUseCase,
    getGameResultDataUseCase: GetGameResultDataUseCase,
    private val gameRepository: GameRepository,
    private val gameApi: GameApi,
    private val roundAPi: RoundApi,
) : ViewModel() {
    private val gameId = savedStateHandle.toRoute<Game>().gameId

    val gameResult = getGameResultDataUseCase(gameId)

    private val _bet = MutableStateFlow(1)
    val bet = _bet.asStateFlow()

    private val _shouldNavToSetup = MutableStateFlow(false)
    val shouldNavToSetup = _shouldNavToSetup.asStateFlow()

    init {
        viewModelScope.launch {
            println(gameApi.getGame(1))
        }
    }

    fun submitResults(playerResults: List<PlayerResult>) {
        if (playerResults.size != 4) throw Exception("The gameResult list size should be 4 but was ${playerResults.size}")

        viewModelScope.launch {
            roundAPi.createRound(
                gameId = gameId,
                bet.value,
                convertToResult(playerResults, bet.value),
            )

            insertResultsUseCase(
                bet.value,
                convertToResult(playerResults, bet.value),
                gameId,
            )
        }
    }

    fun updateBet(bet: Int) {
        _bet.value = bet
    }

    fun startNewGame() {
        // nav to setupScreen
        _shouldNavToSetup.value = true
    }

    fun finishNav() {
        _shouldNavToSetup.value = false
    }

    private fun convertToResult(playerResults: List<PlayerResult>, bet: Int): List<Result> {
        fun String.cardsInHandToInt() =
            toIntOrNull() ?: throw Exception("cardsInHand is not a number")

        val totalProfit = playerResults.filter { !it.isWinner }.sumOf {
            it.cardsInHand.cardsInHandToInt() * bet
        }

        val results = playerResults.map { gameResult ->
            if (gameResult.isWinner) {
                Result(playerId = gameResult.playerId, profit = totalProfit)
            } else {
                val profit = gameResult.cardsInHand.cardsInHandToInt() * bet * -1
                Result(playerId = gameResult.playerId, profit = profit)
            }
        }
        return results
    }
}
