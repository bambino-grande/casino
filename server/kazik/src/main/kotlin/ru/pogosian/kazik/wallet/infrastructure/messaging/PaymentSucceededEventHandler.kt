package ru.pogosian.kazik.wallet.infrastructure.messaging

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.infrastructure.messaging.IntegrationEvent
import ru.pogosian.kazik.common.infrastructure.messaging.IntegrationEventHandler
import ru.pogosian.kazik.payments.api.PAYMENT_SUCCEEDED_EVENT
import ru.pogosian.kazik.payments.api.PURPOSE_WALLET_TOP_UP
import ru.pogosian.kazik.payments.api.PaymentSucceededEvent
import ru.pogosian.kazik.wallet.business.services.TopUpCreditService
import tools.jackson.databind.ObjectMapper

@Component
class PaymentSucceededEventHandler(
    private val topUpCreditService: TopUpCreditService,
    private val objectMapper: ObjectMapper,
) : IntegrationEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override val eventType: String = PAYMENT_SUCCEEDED_EVENT

    override fun handle(event: IntegrationEvent) {
        val payload = objectMapper.readValue(event.payload, PaymentSucceededEvent::class.java)
        if (payload.purpose != PURPOSE_WALLET_TOP_UP) {
            log.info("[WALLET] skip payment {} with purpose {}", payload.paymentId, payload.purpose)
            return
        }
        topUpCreditService.creditTopUp(event.eventId, payload.paymentId, UserId(payload.userId), Rub(payload.amountKopecks))
    }
}
