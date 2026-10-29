package ru.pogosian.kazik.payments.infrastructure.providers.zbank

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "kazik.payments.zbank")
data class ZBankProperties(
    val baseUrl: String,
    val apiKey: String,
    val accountId: String,
    val connectTimeout: Duration,
    val readTimeout: Duration,
) {
    init {
        require(apiKey.isNotBlank()) { "ZBANK_API_KEY is not set" }
        require(accountId.isNotBlank()) { "ZBANK_ACCOUNT_ID is not set" }
    }
}
