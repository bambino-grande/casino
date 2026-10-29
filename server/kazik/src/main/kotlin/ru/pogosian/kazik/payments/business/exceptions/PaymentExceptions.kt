package ru.pogosian.kazik.payments.business.exceptions

import ru.pogosian.kazik.common.business.exceptions.BusinessRuleException
import ru.pogosian.kazik.common.business.exceptions.ConflictException
import ru.pogosian.kazik.common.business.exceptions.DomainValidationException
import ru.pogosian.kazik.common.business.exceptions.ExternalServiceUnavailableException
import ru.pogosian.kazik.common.business.exceptions.NotFoundException
import ru.pogosian.kazik.payments.business.payments.PaymentMethod
import java.util.UUID

class PaymentNotFoundException(id: UUID) : NotFoundException("Payment $id does not exist", "PAYMENT_NOT_FOUND")

class InvalidPaymentAmountException(message: String) : DomainValidationException(message, "INVALID_PAYMENT_AMOUNT")

class InvalidDonationException(message: String) : DomainValidationException(message, "INVALID_DONATION")

class InvalidCardException(message: String) : DomainValidationException(message, "INVALID_CARD")

class PaymentInstrumentRequiredException(method: PaymentMethod) :
    DomainValidationException("Payment via $method requires card details", "CARD_DETAILS_REQUIRED")

class IllegalPaymentTransitionException(status: String, action: String) :
    ConflictException("Payment in status $status cannot $action", "ILLEGAL_PAYMENT_TRANSITION")

class ProviderIdempotencyConflictException(method: PaymentMethod) :
    ConflictException("$method already has an attempt with this Idempotency-Key and different card or amount", "IDEMPOTENCY_KEY_REUSED")

class PaymentDeclinedException(providerCode: String?) :
    BusinessRuleException("Payment was declined by the bank: ${providerCode ?: "unknown reason"}", declineCode(providerCode))

class PaymentProviderNotConfiguredException(method: PaymentMethod) :
    ExternalServiceUnavailableException("Payment provider $method is not configured", "PAYMENT_PROVIDER_NOT_CONFIGURED")

class PaymentProviderUnavailableException(method: PaymentMethod, cause: Throwable?) :
    ExternalServiceUnavailableException(
        "Payment provider $method is temporarily unavailable, retry with the same Idempotency-Key",
        "PAYMENT_PROVIDER_UNAVAILABLE",
    ) {
    init {
        cause?.let { initCause(it) }
    }
}

private fun declineCode(providerCode: String?): String = when (providerCode) {
    "INSUFFICIENT_FUNDS" -> "CARD_INSUFFICIENT_FUNDS"
    "INVALID_CARD_DETAILS" -> "INVALID_CARD_DETAILS"
    "CARD_UNAVAILABLE" -> "CARD_UNAVAILABLE"
    else -> "PAYMENT_DECLINED"
}
