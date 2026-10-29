package ru.pogosian.kazik.payments.presentation.DTO.request

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class CreateTopUpRequest(
    @field:Schema(description = "сумма в рублях; по курсу 1 к 1 столько же наеБАЛЛОВ придёт на кошелёк", example = "500")
    val amount: BigDecimal,
    @field:Schema(description = "учебная карта Z-банка; нигде не сохраняется")
    val card: CardRequest,
)

data class CardRequest(
    @field:Schema(description = "16 цифр", example = "4000000000000002")
    val number: String,
    @field:Schema(example = "10")
    val expiryMonth: Int,
    @field:Schema(description = "полный год", example = "2029")
    val expiryYear: Int,
    @field:Schema(description = "3 цифры", example = "007")
    val cvv: String,
) {
    override fun toString(): String = "CardRequest(number=****${number.takeLast(4)})"
}
