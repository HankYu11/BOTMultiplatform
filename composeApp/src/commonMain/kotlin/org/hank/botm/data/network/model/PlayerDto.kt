package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    val id: Int,
    val name: String,
    val balance: Int,
    val gameId: Int
)
