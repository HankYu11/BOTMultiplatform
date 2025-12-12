package org.hank.botm.data.network.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.sse.deserialize
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.hank.botm.Config
import org.hank.botm.data.network.model.CreateGameDto
import org.hank.botm.data.network.model.GameDetailsDto
import org.hank.botm.data.network.model.GameWithPlayersDto

interface GameApi {
    suspend fun createGame(createGameDto: CreateGameDto): GameWithPlayersDto
    suspend fun getGame(id: Int): GameDetailsDto
    suspend fun headGame(id: Int)
    fun getGameFlow(id: Int): Flow<GameDetailsDto>
}

class GameApiImpl(
    private val httpClient: HttpClient,
) : GameApi {

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

    override fun getGameFlow(id: Int): Flow<GameDetailsDto> = channelFlow {
        if (Config.isDebug) {
            println("[SSE] Starting SSE connection for game $id")
        }
        httpClient.sse("game/$id/sse", deserialize = { typeInfo, jsonString ->
            val serializer = Json.serializersModule.serializer(typeInfo.kotlinType!!)
            Json.decodeFromString(serializer, jsonString)
        }) {
            try {
                while (true) {
                    incoming.collect { event ->
                        if (event.event == "update") {
                            if (Config.isDebug) {
                                println("[SSE] Received update event for game $id")
                            }
                            deserialize<GameDetailsDto>(event.data)?.let {
                                send(it)
                            } ?: run {
                                val error = Exception("Failed to deserialize event $event")
                                if (Config.isDebug) {
                                    println("[SSE] Error: ${error.message}")
                                }
                                close(error)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (Config.isDebug) {
                    println("[SSE] Connection error for game $id: ${e.message}")
                    e.printStackTrace()
                }
                close(e)
            }
        }
        if (Config.isDebug) {
            println("[SSE] SSE connection closed for game $id")
        }
    }
}