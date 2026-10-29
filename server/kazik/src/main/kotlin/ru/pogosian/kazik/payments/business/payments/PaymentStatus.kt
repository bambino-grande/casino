package ru.pogosian.kazik.payments.business.payments

enum class PaymentStatus {
    CREATED,
    SUCCEEDED,
    FAILED,
    EXPIRED,
    ;

    fun canBeSubmitted(): Boolean = this == CREATED || this == EXPIRED
}
