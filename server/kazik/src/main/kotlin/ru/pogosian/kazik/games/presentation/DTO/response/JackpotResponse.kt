package ru.pogosian.kazik.games.presentation.DTO.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class JackpotResponse(
    @field:Schema(description = "текущий пул джекпота в наеБАЛЛАХ")
    val amount: BigDecimal,
    @field:Schema(description = "с какой суммы пул начинается после выигрыша")
    val seed: BigDecimal,
    val recentWins: List<JackpotWinResponse>,
)

data class JackpotWinResponse(
    val roundId: UUID,
    val amount: BigDecimal,
    val wonAt: Instant,
)
