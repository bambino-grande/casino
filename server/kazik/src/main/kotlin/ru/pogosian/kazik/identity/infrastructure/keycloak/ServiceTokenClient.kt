package ru.pogosian.kazik.identity.infrastructure.keycloak

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

@Component
class ServiceTokenClient(
    private val properties: KeycloakAdminProperties,
    private val keycloakRestClient: RestClient,
    private val clock: Clock,
) {
    private val cached = AtomicReference<CachedToken?>(null)

    fun token(): String {
        val current = cached.get()
        if (current != null && clock.instant().isBefore(current.refreshAt)) return current.value
        return synchronized(this) {
            cached.get()?.takeIf { clock.instant().isBefore(it.refreshAt) }?.value ?: fetch().value
        }
    }

    fun invalidate() {
        cached.set(null)
    }

    private fun fetch(): CachedToken {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
            add("client_id", properties.clientId)
            add("client_secret", properties.clientSecret)
        }
        val response = keycloakRestClient.post()
            .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TokenResponse::class.java)
            ?: error("empty token response from Keycloak")
        val token = CachedToken(response.accessToken, clock.instant().plusSeconds((response.expiresIn - 30).coerceAtLeast(5)))
        cached.set(token)
        return token
    }

    private data class CachedToken(val value: String, val refreshAt: Instant)

    data class TokenResponse(
        @com.fasterxml.jackson.annotation.JsonProperty("access_token") val accessToken: String,
        @com.fasterxml.jackson.annotation.JsonProperty("expires_in") val expiresIn: Long,
    )
}
