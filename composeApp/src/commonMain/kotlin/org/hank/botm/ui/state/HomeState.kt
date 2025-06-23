package org.hank.botm.ui.state

import org.hank.botm.domain.model.GameWithDetails
import org.hank.botm.ui.viewmodel.GameError

/**
 * Represents the UI state for the Home screen.
 */
data class HomeState(
    val gameWithDetails: GameWithDetails? = null,
    val bet: Int = 1,
    val gameError: GameError? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
