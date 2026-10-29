package ru.pogosian.kazik.payments.business.providers

import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.payments.business.payments.Card
import ru.pogosian.kazik.payments.business.payments.PaymentMethod
import ru.pogosian.kazik.payments.business.payments.SbpRequisites
import java.util.UUID

interface PaymentProvider {
    val method: PaymentMethod

    fun create(request: ProviderPaymentRequest): ProviderPayment
}

data class ProviderPaymentRequest(
    val operationId: UUID,
    val amount: Rub,
    val description: String,
    val card: Card?,
)

enum class ProviderPaymentStatus {
    SUCCEEDED,
    DECLINED,
}

data class ProviderPayment(
    val providerPaymentId: String,
    val status: ProviderPaymentStatus,
    val declineCode: String?,
    val sbpRequisites: SbpRequisites?,
)
