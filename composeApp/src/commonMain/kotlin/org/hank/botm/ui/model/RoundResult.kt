package org.hank.botm.ui.model

import org.hank.botm.domain.model.Result

data class RoundResult(
    val roundId: Long,
    val results: List<Result>,
)