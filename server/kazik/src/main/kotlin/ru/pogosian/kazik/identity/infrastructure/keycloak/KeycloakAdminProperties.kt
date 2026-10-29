package ru.pogosian.kazik.identity.infrastructure.keycloak

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "kazik.identity.keycloak")
data class KeycloakAdminProperties(
    val baseUrl: String,
    val realm: String,
    val clientId: String,
    val clientSecret: String,
    val connectTimeout: Duration,
    val readTimeout: Duration,
)
