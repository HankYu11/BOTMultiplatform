package org.hank.botm.data.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.hank.botm.Config

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.applyCommonConfiguration() {
    expectSuccess = true

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
        logger = object: Logger {
            override fun log(message: String) {
                // TODO("use Timber")
            }
        }
    }

    defaultRequest {
        header("Content-Type", "application/json")
        url(Config.baseUrl)
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