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
     * Determines if user is eligible for online features.
     * This can be based on subscription tier, A/B testing, or feature flag.
     * When false, the app operates in pure offline mode.
     * Future: Will be determined by user authentication.
     */
    var isEligibleForOnline: Boolean = true
}

/**
 * Generates unique local IDs for offline mode.
 * Uses negative numbers to distinguish from server-generated positive IDs.
 *
 * ID Ranges:
 * - Server IDs: Positive (1, 2, 3, ...)
 * - Offline IDs: Negative (-1, -2, -3, ...)
 *
 * This ensures offline-created entities never conflict with server entities.
 *
 * Note: Uses platform-specific atomic operations for thread safety.
 */
expect object OfflineIdGenerator {
    /**
     * Generate a unique negative ID for offline entities.
     * Returns negative integers: -1, -2, -3, etc.
     * Thread-safe.
     */
    fun nextId(): Int

    /**
     * Check if an ID is an offline-generated ID (negative).
     */
    fun isOfflineId(id: Int): Boolean

    /**
     * Reset counter (useful for testing).
     */
    fun reset()
}

/**
 * Get current time in milliseconds.
 * Platform-specific implementation.
 */
expect fun currentTimeMillis(): Long