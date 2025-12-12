package org.hank.botm.domain.model

/**
 * Sealed class hierarchy for application errors.
 * Provides typed errors with user-friendly messages for better error handling.
 */
sealed class AppError(
    val userMessage: String,
    val cause: Throwable? = null
) : Exception(userMessage, cause) {

    /**
     * Network-related errors (connection issues, timeouts, server errors)
     */
    sealed class NetworkError(
        userMessage: String,
        cause: Throwable? = null
    ) : AppError(userMessage, cause) {

        class ConnectionFailed(cause: Throwable? = null) : NetworkError(
            userMessage = "Unable to connect to server. Please check your internet connection.",
            cause = cause
        )

        class Timeout(cause: Throwable? = null) : NetworkError(
            userMessage = "Request timed out. Please try again.",
            cause = cause
        )

        class ServerError(
            val statusCode: Int,
            cause: Throwable? = null
        ) : NetworkError(
            userMessage = "Server error occurred (code: $statusCode). Please try again later.",
            cause = cause
        )

        class GameNotFound(val gameId: Int) : NetworkError(
            userMessage = "Game with ID $gameId was not found."
        )

        class SseConnectionLost(cause: Throwable? = null) : NetworkError(
            userMessage = "Lost connection to game updates. Reconnecting...",
            cause = cause
        )
    }

    /**
     * Database-related errors (insert failures, query errors)
     */
    sealed class DatabaseError(
        userMessage: String,
        cause: Throwable? = null
    ) : AppError(userMessage, cause) {

        class InsertFailed(cause: Throwable? = null) : DatabaseError(
            userMessage = "Failed to save data. Please try again.",
            cause = cause
        )

        class QueryFailed(cause: Throwable? = null) : DatabaseError(
            userMessage = "Failed to load data. Please try again.",
            cause = cause
        )
    }

    /**
     * Validation errors (invalid input, business rule violations)
     */
    sealed class ValidationError(
        userMessage: String,
        cause: Throwable? = null
    ) : AppError(userMessage, cause) {

        class InvalidPlayerCount(val expected: Int, val actual: Int) : ValidationError(
            userMessage = "Expected $expected players but got $actual."
        )

        class InvalidCardsInHand(val playerName: String) : ValidationError(
            userMessage = "Invalid cards value for $playerName. Please enter a number."
        )

        class InvalidGameId : ValidationError(
            userMessage = "Please enter a valid game ID."
        )

        class EmptyPlayerName(val playerIndex: Int) : ValidationError(
            userMessage = "Player ${playerIndex + 1} name cannot be empty."
        )
    }

    /**
     * Generic unexpected error
     */
    class Unexpected(cause: Throwable? = null) : AppError(
        userMessage = "An unexpected error occurred. Please try again.",
        cause = cause
    )
}

/**
 * Extension function to convert generic exceptions to AppError.
 * Uses message-based detection for platform-specific exceptions in common code.
 */
fun Throwable.toAppError(): AppError = when {
    this is AppError -> this
    this.message?.contains("UnknownHost", ignoreCase = true) == true ->
        AppError.NetworkError.ConnectionFailed(this)
    this.message?.contains("timeout", ignoreCase = true) == true ->
        AppError.NetworkError.Timeout(this)
    this.message?.contains("Connection refused", ignoreCase = true) == true ->
        AppError.NetworkError.ConnectionFailed(this)
    this.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
        AppError.NetworkError.ConnectionFailed(this)
    else -> AppError.Unexpected(this)
}
