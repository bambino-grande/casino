package ru.pogosian.kazik.payments.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.pogosian.kazik.common.business.rubOf
import ru.pogosian.kazik.payments.business.payments.PaymentMethod
import ru.pogosian.kazik.payments.business.payments.PaymentPurpose
import ru.pogosian.kazik.payments.business.payments.PaymentRoutingPolicy
import ru.pogosian.kazik.payments.business.payments.PurposeRule
import ru.pogosian.kazik.payments.business.providers.PaymentProvider
import ru.pogosian.kazik.payments.business.providers.PaymentProviderRegistry
import ru.pogosian.kazik.payments.infrastructure.providers.sbp.SbpProperties
import ru.pogosian.kazik.payments.infrastructure.providers.zbank.ZBankProperties
import java.math.BigDecimal
import java.time.Duration

@ConfigurationProperties(prefix = "kazik.payments")
data class PaymentsProperties(
    val topUp: PurposeProperties,
    val donation: PurposeProperties,
)

data class PurposeProperties(
    val minAmount: BigDecimal,
    val maxAmount: BigDecimal,
    val timeToPay: Duration,
)

@ConfigurationProperties(prefix = "kazik.payments.outbox")
data class OutboxProperties(
    val batchSize: Int,
    val lease: Duration,
    val retryBackoff: Duration,
)

@Configuration
@EnableConfigurationProperties(PaymentsProperties::class, OutboxProperties::class, ZBankProperties::class, SbpProperties::class)
class PaymentsConfiguration {
    @Bean
    fun paymentProviderRegistry(providers: List<PaymentProvider>): PaymentProviderRegistry = PaymentProviderRegistry(providers)

    @Bean
    fun paymentRoutingPolicy(properties: PaymentsProperties): PaymentRoutingPolicy =
        PaymentRoutingPolicy(
            mapOf(
                PaymentPurpose.WALLET_TOP_UP to properties.topUp.toRule(PaymentMethod.Z_BANK),
                PaymentPurpose.AUTHOR_DONATION to properties.donation.toRule(PaymentMethod.SBP),
            ),
        )

    private fun PurposeProperties.toRule(method: PaymentMethod): PurposeRule =
        PurposeRule(method, rubOf(minAmount), rubOf(maxAmount), timeToPay)
}
