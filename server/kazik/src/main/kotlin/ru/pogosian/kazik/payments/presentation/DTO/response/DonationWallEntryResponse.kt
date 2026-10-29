package ru.pogosian.kazik.payments.presentation.DTO.response

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class DonationWallEntryResponse(
    val paymentId: UUID,
    val donorName: String,
    val message: String?,
    val amount: BigDecimal,
    val donatedAt: Instant,
)
