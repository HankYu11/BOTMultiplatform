package org.hank.botm.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.request.head
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.hank.botm.Config

/**
 * Checks server connectivity by making a lightweight HEAD request.
 */
interface ConnectivityChecker {
    val connectionState: StateFlow<ConnectionState>

    /**
     * Check if the server is reachable.
     * Updates connectionState and returns true if connected.
     */
    suspend fun checkConnectivity(): Boolean
}

class ConnectivityCheckerImpl(
    private val httpClient: HttpClient
) : ConnectivityChecker {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Unknown)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun checkConnectivity(): Boolean {
        return try {
            // Simple HEAD request to check server availability
            // Using the game endpoint as a health check
            httpClient.head(Config.baseUrl + "game/1")
            _connectionState.value = ConnectionState.Connected
            true
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Disconnected
            if (Config.isDebug) {
                println("[ConnectivityChecker] Connection failed: ${e.message}")
            }
            false
        }
    }
}
