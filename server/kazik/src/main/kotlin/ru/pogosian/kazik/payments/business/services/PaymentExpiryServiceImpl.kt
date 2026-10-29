package ru.pogosian.kazik.payments.business.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.pogosian.kazik.payments.infrastructure.repository.PaymentRepository
import java.time.Clock

@Service
class PaymentExpiryServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val clock: Clock,
) : PaymentExpiryService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun expireOverdue(): Int {
        val expired = paymentRepository.expireOverdue(clock.instant())
        if (expired > 0) log.info("[PAYMENTS] {} unpaid payments expired", expired)
        return expired
    }
}
