package org.hank.botm.domain.usecase

import org.hank.botm.data.repository.PlayerRepository
import org.hank.botm.ui.model.GameResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetGameResultDataUseCase (
    private val playerRepository: PlayerRepository,
    private val getRoundResultUseCase: GetRoundResultUseCase,
) {
    operator fun invoke(
        gameId: Int,
    ) : Flow<GameResult> {
        val playersFlow = playerRepository.getCurrentGamePlayers()
        val roundResultsFlow = getRoundResultUseCase(gameId)

        return combine(playersFlow, roundResultsFlow) { players, roundResults ->
            GameResult(
                players = players,
                roundResults = roundResults
            )
        }
    }
}