package org.hank.botm.ui.state

import org.hank.botm.domain.model.CreateGame

/**
 * Represents the UI state for the Setup screen.
 */
data class SetupState(
    val createGame: CreateGame = CreateGame(),
    val gameId: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
