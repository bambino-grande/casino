package ru.pogosian.kazik.payments.business.providers

import ru.pogosian.kazik.payments.business.exceptions.PaymentProviderNotConfiguredException
import ru.pogosian.kazik.payments.business.payments.PaymentMethod

class PaymentProviderRegistry(providers: List<PaymentProvider>) {
    private val providersByMethod = providers.associateBy { it.method }

    init {
        require(providersByMethod.size == providers.size) { "only one provider per payment method" }
    }

    fun get(method: PaymentMethod): PaymentProvider =
        providersByMethod[method] ?: throw PaymentProviderNotConfiguredException(method)
}
