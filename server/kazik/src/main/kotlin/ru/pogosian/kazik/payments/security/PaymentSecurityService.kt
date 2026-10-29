package ru.pogosian.kazik.payments.security

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import ru.pogosian.kazik.payments.infrastructure.repository.PaymentRepository
import java.util.UUID

@Component
class PaymentSecurityService(
    private val paymentRepository: PaymentRepository,
) {
    fun isPaymentOwner(id: UUID, authentication: Authentication): Boolean {
        val subject = (authentication as? JwtAuthenticationToken)?.token?.subject ?: return false
        val payment = paymentRepository.findById(id) ?: return true
        return payment.userId.value.toString() == subject
    }
}
