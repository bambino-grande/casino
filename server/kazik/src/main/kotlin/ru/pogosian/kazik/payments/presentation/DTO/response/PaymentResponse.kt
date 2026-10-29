package ru.pogosian.kazik.payments.presentation.DTO.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PaymentResponse(
    val id: UUID,
    @field:Schema(description = "WALLET_TOP_UP — пополнение наеБАЛЛОВ, AUTHOR_DONATION — донат автору")
    val purpose: String,
    @field:Schema(description = "Z_BANK или SBP")
    val method: String,
    @field:Schema(description = "сумма в рублях")
    val amount: BigDecimal,
    @field:Schema(description = "CREATED, SUCCEEDED, FAILED, EXPIRED")
    val status: String,
    @field:Schema(description = "реквизиты для перевода доната по СБП")
    val sbpTransfer: SbpTransferResponse?,
    val failureReason: String?,
    val expiresAt: Instant,
    val createdAt: Instant,
    val succeededAt: Instant?,
    val donation: DonationResponse?,
)

data class SbpTransferResponse(
    val phone: String,
    val bank: String,
    val recipient: String,
    @field:Schema(description = "обязательно указать в комментарии к переводу", example = "KZK-A1B2C3")
    val comment: String,
)

data class DonationResponse(
    val donorName: String?,
    val message: String?,
    val showOnWall: Boolean,
)
