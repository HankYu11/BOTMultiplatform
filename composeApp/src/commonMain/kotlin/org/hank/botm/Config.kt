package org.hank.botm

enum class Environment {
    DEV,
    PROD,
}

object Config {
    // temp manual approach for using local environment
    private var environment: Environment = Environment.PROD

    val isDebug: Boolean
        get() = environment == Environment.DEV

    val baseUrl = when (environment) {
        // 10.0.2.2 is the setup for emulator
        Environment.DEV -> "http://10.0.2.2:8080/"
        Environment.PROD -> "https://botktor-production.up.railway.app/"
    }

    /**
     * Offline mode toggle.
     * When true, the app works without a server - all data is stored locally.
     * Easy to change per user tier or build variant.
     */
    var offlineMode: Boolean = false
}

/**
 * Generates unique local IDs for offline mode.
 * Uses negative numbers to distinguish from server-generated positive IDs.
 * Note: No synchronization needed as game creation is sequential via user interaction.
 */
object OfflineIdGenerator {
    private var gameIdCounter = -1

    /**
     * Generate a unique game ID for offline mode.
     * Returns negative integers: -2, -3, -4, etc.
     */
    fun nextGameId(): Int = --gameIdCounter

    /**
     * Generate player IDs for a game.
     * Uses game ID as base to ensure uniqueness across games.
     * Example: game -2 → players -20, -21, -22, -23
     */
    fun playerIds(gameId: Int): List<Int> {
        val base = gameId * 10
        return (0 until 4).map { base - it }
    }

    /**
     * Reset counters (useful for testing).
     */
    fun reset() {
        gameIdCounter = -1
    }
}