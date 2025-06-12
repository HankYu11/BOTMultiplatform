package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateGameDto(
    val playerNames: List<String>,
)
