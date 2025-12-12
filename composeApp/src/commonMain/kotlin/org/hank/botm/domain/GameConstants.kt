package org.hank.botm.domain

/**
 * Centralized constants for the Big Old Two game.
 * This ensures consistency across the codebase and makes it easier to modify game rules.
 */
object GameConstants {
    /**
     * Number of players in a Big Old Two game.
     */
    const val PLAYER_COUNT = 4

    /**
     * Minimum bet amount allowed.
     */
    const val MIN_BET = 1

    /**
     * Maximum cards a player can have (full hand).
     */
    const val MAX_CARDS_IN_HAND = 13

    /**
     * Default bet amount when starting a new game.
     */
    const val DEFAULT_BET = 1
}
