package org.hank.botm.domain.usecase

import org.hank.botm.data.repository.PlayerRepository
import org.hank.botm.data.repository.RoundRepository
import org.hank.botm.domain.model.Result
import org.hank.botm.domain.model.Round

class InsertResultsUseCase (
    private val playerRepository: PlayerRepository,
    private val roundRepository: RoundRepository,
) {
    suspend operator fun invoke(bet: Int, results: List<Result>, gameId: Long) {
        if (results.size != 4) throw Exception("The Result list size should be 4 but was ${results.size}")

        val players = results
            .map { result ->
                playerRepository.getPlayer(result.playerId).let { player ->
                    player.copy(balance = player.balance + result.profit)
                }
            }

        roundRepository.insertRound(
            round = Round(bet = bet, gameId = gameId),
            results = results,
            players = players
        )
    }
}