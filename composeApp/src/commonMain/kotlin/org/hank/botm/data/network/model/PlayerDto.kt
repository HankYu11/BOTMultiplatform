package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable
import org.hank.botm.data.database.model.PlayerEntity

@Serializable
data class PlayerDto(
    val id: Int,
    val name: String,
    val balance: Int,
    val gameId: Int
)

fun PlayerDto.toEntity() = PlayerEntity(id, name, balance, gameId)