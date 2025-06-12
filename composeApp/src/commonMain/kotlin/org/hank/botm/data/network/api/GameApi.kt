package org.hank.botm.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.hank.botm.data.network.model.CreateGameDto
import org.hank.botm.data.network.model.GameDetailsDto
import org.hank.botm.data.network.model.GameWithPlayers

interface GameApi {
    suspend fun createGame(createGameDto: CreateGameDto): GameWithPlayers
    suspend fun getGame(id: Int): GameDetailsDto
}

class GameApiImpl(
    private val httpClient: HttpClient,
) : GameApi{

    override suspend fun createGame(createGameDto: CreateGameDto): GameWithPlayers {
        return httpClient.post("game/create") {
            setBody(createGameDto)
        }.body()
    }

    override suspend fun getGame(id: Int): GameDetailsDto {
        return httpClient.get("game/$id").body()
    }
}