package org.hank.botm.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.hank.botm.data.network.model.CreateRoundDto
import org.hank.botm.domain.model.Result
import org.hank.botm.domain.model.asPlayerResultDto

class RoundApi(
    private val client: HttpClient,
) {

    suspend fun createRound(gameId: Int, bet: Int, results: List<Result>) {
        client.post("round/create") {
            setBody(CreateRoundDto(gameId, bet, results.map { it.asPlayerResultDto() }))
        }
    }
}