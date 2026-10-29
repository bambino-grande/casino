package ru.pogosian.kazik.payments.api

import java.time.Instant
import java.util.UUID

const val PAYMENT_SUCCEEDED_EVENT = "payments.payment-succeeded.v1"

const val PURPOSE_WALLET_TOP_UP = "WALLET_TOP_UP"

data class PaymentSucceededEvent(
    val paymentId: UUID,
    val userId: UUID,
    val purpose: String,
    val amountKopecks: Long,
    val succeededAt: Instant,
)
