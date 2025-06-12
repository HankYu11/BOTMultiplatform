package org.hank.botm.ui.model

import org.hank.botm.domain.model.Result

data class RoundResult(
    val roundId: Int,
    val results: List<Result>,
)