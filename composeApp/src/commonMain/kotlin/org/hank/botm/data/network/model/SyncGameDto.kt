package org.hank.botm.data.network.model

import kotlinx.serialization.Serializable

/**
 * Request to sync a complete offline game to the server.
 * The server will create the game with all players, rounds, and results,
 * returning a GameDetailsDto with server-generated IDs.
 */
@Serializable
data class SyncGameRequest(
    val players: List<SyncPlayerRequest>,
    val rounds: List<SyncRoundRequest>,
)

/**
 * Player data for sync request.
 * Only needs the name since balance is calculated from results.
 */
@Serializable
data class SyncPlayerRequest(
    val name: String,
)

/**
 * Round data for sync request.
 * Results reference players by index (0-3) matching the players list order.
 */
@Serializable
data class SyncRoundRequest(
    val bet: Int,
    val results: List<SyncResultRequest>,
)

/**
 * Result data for sync request.
 * Uses playerIndex to reference player position in the players list.
 */
@Serializable
data class SyncResultRequest(
    val playerIndex: Int,
    val profit: Int,
)
