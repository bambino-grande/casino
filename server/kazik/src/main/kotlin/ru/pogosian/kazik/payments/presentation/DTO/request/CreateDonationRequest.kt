package ru.pogosian.kazik.payments.presentation.DTO.request

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class CreateDonationRequest(
    @field:Schema(description = "сумма доната в рублях", example = "300")
    val amount: BigDecimal,
    @field:Schema(description = "как подписать донат на стене, до 64 символов; пусто — аноним", example = "Капибарофил")
    val donorName: String?,
    @field:Schema(description = "сообщение автору, до 500 символов", example = "за голых капибар в 4K")
    val message: String?,
    @field:Schema(description = "показывать ли донат на стене донатеров", example = "true")
    val showOnWall: Boolean,
)
