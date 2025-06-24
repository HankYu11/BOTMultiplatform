package org.hank.botm.data.network

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.hank.botm.Config
import kotlin.time.Duration.Companion.minutes

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.applyCommonConfiguration() {
    expectSuccess = true

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
        logger = object: Logger {
            override fun log(message: String) {
            }
        }
    }

    install(SSE)

    defaultRequest {
        header("Content-Type", "application/json")
        url(Config.baseUrl)
    }

    install(HttpTimeout) {
        socketTimeoutMillis = 1.minutes.inWholeMilliseconds
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
}