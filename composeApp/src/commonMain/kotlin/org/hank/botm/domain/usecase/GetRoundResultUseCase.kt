package org.hank.botm.domain.usecase

import org.hank.botm.data.database.model.asDomain
import org.hank.botm.data.repository.RoundRepository
import org.hank.botm.ui.model.RoundResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetRoundResultUseCase (
    private val roundRepository: RoundRepository,
) {
    operator fun invoke(
        gameId: Long
    ): Flow<List<RoundResult>> {
        return roundRepository.getGameRoundWithResults(gameId)
            .map { listOfRoundWithResults ->
                listOfRoundWithResults.map { roundWithResults ->
                    RoundResult(
                        roundId = roundWithResults.round.id,
                        results = roundWithResults.results.map { it.asDomain() }
                    )
                }
            }
    }
}