package org.hank.botm.data.sync

/**
 * Represents the current state of sync operations.
 */
sealed class SyncState {
    /** No sync operation in progress */
    data object Idle : SyncState()

    /** Sync operation is currently running */
    data object Syncing : SyncState()

    /** Sync completed successfully */
    data class Success(val timestamp: Long) : SyncState()

    /** Sync failed with an error */
    data class Error(
        val message: String,
        val retryable: Boolean = true
    ) : SyncState()
}

/**
 * Represents the current connection state to the server.
 */
sealed class ConnectionState {
    /** Connection state has not been determined yet */
    data object Unknown : ConnectionState()

    /** Successfully connected to the server */
    data object Connected : ConnectionState()

    /** Unable to reach the server */
    data object Disconnected : ConnectionState()
}
