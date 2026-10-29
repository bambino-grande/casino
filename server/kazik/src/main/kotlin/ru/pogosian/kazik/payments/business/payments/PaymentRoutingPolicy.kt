package ru.pogosian.kazik.payments.business.payments

import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.payments.business.exceptions.InvalidPaymentAmountException
import java.time.Duration

data class PurposeRule(
    val method: PaymentMethod,
    val minAmount: Rub,
    val maxAmount: Rub,
    val timeToPay: Duration,
)

class PaymentRoutingPolicy(private val rules: Map<PaymentPurpose, PurposeRule>) {
    init {
        require(rules.keys == PaymentPurpose.entries.toSet()) { "every payment purpose needs a routing rule" }
    }

    fun ruleFor(purpose: PaymentPurpose): PurposeRule = rules.getValue(purpose)

    fun validateAmount(purpose: PaymentPurpose, amount: Rub) {
        val rule = ruleFor(purpose)
        if (amount < rule.minAmount || amount > rule.maxAmount) {
            throw InvalidPaymentAmountException("Amount for $purpose must be between ${rule.minAmount} and ${rule.maxAmount} RUB")
        }
    }
}
