package org.hank.botm.data.database.model

import androidx.room.Embedded
import androidx.room.Relation
import org.hank.botm.domain.model.GameWithDetails

data class GameWithDetailsEntity(
    @Embedded val game: GameEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "gameId"
    )
    val players: List<PlayerEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "gameId",
        entity = RoundEntity::class
    )
    val roundsWithResults: List<RoundWithResultsEntity>
)

fun GameWithDetailsEntity.asDomain() = GameWithDetails(
    gameId = game.id,
    players = players.map { it.asDomain() },
    roundsWithResults = roundsWithResults.map { it.asDomain() },
)