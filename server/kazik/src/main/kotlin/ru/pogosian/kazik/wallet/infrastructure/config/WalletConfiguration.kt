package ru.pogosian.kazik.wallet.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.pogosian.kazik.wallet.business.rates.TopUpRate

@ConfigurationProperties(prefix = "kazik.wallet")
data class WalletProperties(
    val naeBallsPerRub: Long,
)

@Configuration
@EnableConfigurationProperties(WalletProperties::class)
class WalletConfiguration {
    @Bean
    fun topUpRate(properties: WalletProperties): TopUpRate = TopUpRate(properties.naeBallsPerRub)
}
