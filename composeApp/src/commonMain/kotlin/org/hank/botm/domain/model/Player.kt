package org.hank.botm.domain.model

import org.hank.botm.data.database.model.PlayerEntity

data class Player(
    val id: Long = 0,
    val gameId: Long,
    val balance: Int = 0,
    val name: String,
)

fun Player.asEntity(): PlayerEntity = PlayerEntity(id, gameId, name, balance)