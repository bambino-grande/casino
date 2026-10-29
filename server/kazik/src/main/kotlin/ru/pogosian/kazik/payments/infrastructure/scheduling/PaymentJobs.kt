package ru.pogosian.kazik.payments.infrastructure.scheduling

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.pogosian.kazik.payments.business.services.PaymentExpiryService
import ru.pogosian.kazik.payments.infrastructure.messaging.OutboxRelay

@Component
@ConditionalOnProperty(prefix = "kazik.scheduling", name = ["enabled"], havingValue = "true")
class PaymentJobs(
    private val outboxRelay: OutboxRelay,
    private val paymentExpiryService: PaymentExpiryService,
) {
    @Scheduled(fixedDelayString = "\${kazik.payments.outbox.fixed-delay}")
    fun publishOutbox() {
        outboxRelay.publishPending()
    }

    @Scheduled(fixedDelayString = "\${kazik.payments.expiry.fixed-delay}")
    fun expireOverduePayments() {
        paymentExpiryService.expireOverdue()
    }
}
