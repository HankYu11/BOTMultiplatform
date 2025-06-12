package org.hank.botm.domain.model

import org.hank.botm.data.database.model.PlayerEntity

data class Player(
    val id: Int = 0,
    val name: String,
    val balance: Int = 0,
    val gameId: Int,
)

fun Player.asEntity(): PlayerEntity = PlayerEntity(id, name, balance, gameId)