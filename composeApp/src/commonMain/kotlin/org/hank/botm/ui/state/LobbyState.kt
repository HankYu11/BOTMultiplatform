package org.hank.botm.ui.state

data class LobbyState(
    val showJoinGameDialog: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
