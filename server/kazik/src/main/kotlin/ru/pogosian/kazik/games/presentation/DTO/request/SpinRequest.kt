package ru.pogosian.kazik.games.presentation.DTO.request

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class SpinRequest(
    @field:Schema(description = "ставка в наеБАЛЛАХ, не больше двух знаков после запятой", example = "10")
    val bet: BigDecimal,
)
