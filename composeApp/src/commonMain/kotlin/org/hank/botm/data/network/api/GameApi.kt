package org.hank.botm.data.network.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.hank.botm.data.network.model.CreateGameDto
import org.hank.botm.data.network.model.GameDetailsDto
import org.hank.botm.data.network.model.GameWithPlayersDto

interface GameApi {
    suspend fun createGame(createGameDto: CreateGameDto): GameWithPlayersDto
    suspend fun getGame(id: Int): GameDetailsDto
    suspend fun headGame(id: Int)
}

class GameApiImpl(
    private val httpClient: HttpClient,
) : GameApi{

    override suspend fun createGame(createGameDto: CreateGameDto): GameWithPlayersDto {
        return httpClient.post("game/create") {
            setBody(createGameDto)
        }.body()
    }

    override suspend fun getGame(id: Int): GameDetailsDto {
        return httpClient.get("game/$id").body()
    }

    override suspend fun headGame(id: Int) {
        httpClient.head("game/$id")
    }
}