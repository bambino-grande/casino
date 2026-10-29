package ru.pogosian.kazik.rewards.presentation.DTO.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class PrizeResponse(
    val prizeId: UUID,
    @field:Schema(description = "раунд, за джекпот в котором выдан приз")
    val roundId: UUID,
    @field:Schema(description = "адрес картинки-приза в API; открывается с тем же токеном", example = "/api/v1/rewards/prizes/0199.../image")
    val imageUrl: String,
    val grantedAt: Instant,
)
