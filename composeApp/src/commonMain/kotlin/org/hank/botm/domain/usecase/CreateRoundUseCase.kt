package org.hank.botm.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.hank.botm.data.repository.RoundRepository
import org.hank.botm.domain.model.Result
import org.hank.botm.ui.model.PlayerResult

/**
 * Use case for creating a new round with player results.
 * This class encapsulates the business logic for creating a round,
 * which was previously in the HomeViewModel.
 */
class CreateRoundUseCase(
    private val roundRepository: RoundRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Create a new round with the given parameters.
     *
     * @param gameId The ID of the game
     * @param bet The bet amount
     * @param playerResults The results for each player
     * @return A Result indicating success or failure
     */
    suspend operator fun invoke(
        gameId: Int,
        bet: Int,
        playerResults: List<PlayerResult>
    ): kotlin.Result<Unit> = withContext(ioDispatcher) {
        if (playerResults.size != 4) {
            return@withContext kotlin.Result.failure(
                IllegalArgumentException("The gameResult list size should be 4 but was ${playerResults.size}")
            )
        }
        
        val results = convertToResult(playerResults, bet)
        return@withContext roundRepository.insertRound(gameId, bet, results)
    }
    
    /**
     * Convert player results to domain model results.
     *
     * @param playerResults The UI model player results
     * @param bet The bet amount
     * @return A list of domain model results
     */
    private fun convertToResult(playerResults: List<PlayerResult>, bet: Int): List<Result> {
        fun String.cardsInHandToInt() =
            toIntOrNull() ?: throw IllegalArgumentException("cardsInHand is not a number")
        
        val totalProfit = playerResults.filter { !it.isWinner }.sumOf {
            it.cardsInHand.cardsInHandToInt() * bet
        }
        
        return playerResults.map { gameResult ->
            if (gameResult.isWinner) {
                Result(playerId = gameResult.playerId, profit = totalProfit)
            } else {
                val profit = gameResult.cardsInHand.cardsInHandToInt() * bet * -1
                Result(playerId = gameResult.playerId, profit = profit)
            }
        }
    }
}