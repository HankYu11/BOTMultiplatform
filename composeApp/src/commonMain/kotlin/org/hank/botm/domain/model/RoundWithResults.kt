package org.hank.botm.domain.model

data class RoundWithResults(
    val round: Round,
    val results: List<Result>
)
