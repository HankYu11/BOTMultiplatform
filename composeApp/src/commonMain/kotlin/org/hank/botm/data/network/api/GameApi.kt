package org.hank.botm.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.hank.botm.data.network.model.CreateGameDto
import org.hank.botm.data.network.model.GameDetailsDto

class GameApi(
    private val httpClient: HttpClient,
) {

    suspend fun createGame(createGameDto: CreateGameDto): String {
        return httpClient.post("game/create") {
            setBody(createGameDto)
        }.body()
    }

    suspend fun getGame(id: Int): GameDetailsDto {
        return httpClient.get("game/$id").body()
    }
}