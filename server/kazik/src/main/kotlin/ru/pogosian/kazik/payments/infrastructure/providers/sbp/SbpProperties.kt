package ru.pogosian.kazik.payments.infrastructure.providers.sbp

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kazik.payments.sbp")
data class SbpProperties(
    val phone: String,
    val bank: String,
    val recipient: String,
) {
    init {
        require(phone.isNotBlank()) { "SBP_PHONE is not set" }
    }
}
