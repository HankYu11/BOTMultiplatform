package org.hank.botm.domain.model

import org.hank.botm.data.database.model.GameEntity

data class Game(
    val id: Long = 0,
    val isFinished: Boolean = false,
)

fun Game.asEntity() = GameEntity(id = id)
