package ru.pogosian.kazik.wallet.presentation.DTO.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.UUID

data class WalletResponse(
    @field:Schema(description = "id пользователя")
    val userId: UUID,
    @field:Schema(description = "баланс в наеБАЛЛАХ", example = "150.00")
    val balance: BigDecimal,
    @field:Schema(description = "название валюты", example = "наеБАЛЛЫ")
    val currency: String,
)
