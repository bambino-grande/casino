package ru.pogosian.kazik.payments.business.payments

import ru.pogosian.kazik.payments.business.exceptions.InvalidCardException

private val CARD_NUMBER = Regex("[0-9]{16}")

private val CARD_CVV = Regex("[0-9]{3}")

class Card(
    val number: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
) {
    init {
        if (!CARD_NUMBER.matches(number)) throw InvalidCardException("card number must be 16 digits")
        if (expiryMonth !in 1..12) throw InvalidCardException("expiry month must be 1..12")
        if (expiryYear !in 2000..9999) throw InvalidCardException("expiry year must be a full year")
        if (!CARD_CVV.matches(cvv)) throw InvalidCardException("cvv must be 3 digits")
    }

    val masked: String get() = "**** " + number.takeLast(4)

    override fun toString(): String = "Card($masked)"
}
