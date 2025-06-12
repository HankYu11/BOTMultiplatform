package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable
import org.hank.botm.data.database.model.GameEntity

@Serializable
data class GameDto(
    val id: Int,
)

fun GameDto.toEntity() = GameEntity(id = id)
