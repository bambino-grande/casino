package ru.pogosian.kazik.payments.infrastructure.providers.sbp

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.pogosian.kazik.payments.business.payments.PaymentMethod
import ru.pogosian.kazik.payments.business.payments.SbpRequisites
import ru.pogosian.kazik.payments.business.providers.PaymentProvider
import ru.pogosian.kazik.payments.business.providers.ProviderPayment
import ru.pogosian.kazik.payments.business.providers.ProviderPaymentRequest
import ru.pogosian.kazik.payments.business.providers.ProviderPaymentStatus

@Component
class SbpTransferPaymentProvider(
    private val properties: SbpProperties,
) : PaymentProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    override val method: PaymentMethod = PaymentMethod.SBP

    override fun create(request: ProviderPaymentRequest): ProviderPayment {
        val comment = "KZK-" + request.operationId.toString().replace("-", "").takeLast(6).uppercase()
        log.info("[SBP] donation of operation {} accepted without verification, transfer comment {}", request.operationId, comment)
        return ProviderPayment(
            providerPaymentId = "sbp-${request.operationId}",
            status = ProviderPaymentStatus.SUCCEEDED,
            declineCode = null,
            sbpRequisites = SbpRequisites(properties.phone, properties.bank, properties.recipient, comment),
        )
    }
}
