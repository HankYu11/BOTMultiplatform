package org.hank.botm.domain.model

import org.hank.botm.data.database.model.RoundEntity

data class Round(
    val id: Int = 0,
    val bet: Int,
    val gameId: Int,
)

// We set the RoundEntity id to 0 for autoGenerate the primary key.
fun Round.asEntity() = RoundEntity(bet = bet, gameId = gameId)
