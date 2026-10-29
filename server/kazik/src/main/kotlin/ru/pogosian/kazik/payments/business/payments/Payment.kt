package ru.pogosian.kazik.payments.business.payments

import ru.pogosian.kazik.common.business.IdempotencyKey
import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.payments.business.exceptions.IllegalPaymentTransitionException
import java.time.Instant
import java.util.UUID

data class Payment(
    val id: UUID,
    val operationId: UUID,
    val userId: UserId,
    val idempotencyKey: IdempotencyKey,
    val method: PaymentMethod,
    val purpose: PaymentPurpose,
    val amount: Rub,
    val status: PaymentStatus,
    val providerPaymentId: String?,
    val sbpRequisites: SbpRequisites?,
    val failureReason: String?,
    val expiresAt: Instant,
    val succeededAt: Instant?,
    val createdAt: Instant,
    val version: Long,
) {
    fun isSameRequest(purpose: PaymentPurpose, amount: Rub): Boolean = this.purpose == purpose && this.amount == amount

    fun succeed(providerPaymentId: String, sbpRequisites: SbpRequisites?, now: Instant): Payment {
        checkCanBeSubmitted("succeed")
        return copy(status = PaymentStatus.SUCCEEDED, providerPaymentId = providerPaymentId, sbpRequisites = sbpRequisites, succeededAt = now)
    }

    fun decline(providerPaymentId: String, reason: String): Payment {
        checkCanBeSubmitted("fail")
        return copy(status = PaymentStatus.FAILED, providerPaymentId = providerPaymentId, failureReason = reason)
    }

    private fun checkCanBeSubmitted(action: String) {
        if (!status.canBeSubmitted()) throw IllegalPaymentTransitionException(status.name, action)
    }
}
