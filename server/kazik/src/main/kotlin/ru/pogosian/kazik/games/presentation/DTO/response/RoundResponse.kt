package ru.pogosian.kazik.games.presentation.DTO.response

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class RoundResponse(
    val roundId: UUID,
    val userId: UUID,
    val machineCode: String,
    val paytableVersion: Int,
    val reels: List<String>,
    val bet: BigDecimal,
    val win: BigDecimal,
    val jackpotWin: BigDecimal,
    val createdAt: Instant,
)
