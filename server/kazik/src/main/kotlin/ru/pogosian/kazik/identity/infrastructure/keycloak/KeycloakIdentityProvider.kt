package ru.pogosian.kazik.identity.infrastructure.keycloak

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import ru.pogosian.kazik.identity.business.accounts.NewAccount
import ru.pogosian.kazik.identity.business.exceptions.EmailTakenException
import ru.pogosian.kazik.identity.business.exceptions.IdentityProviderUnavailableException
import ru.pogosian.kazik.identity.business.exceptions.UsernameTakenException
import ru.pogosian.kazik.identity.business.exceptions.WeakPasswordException
import ru.pogosian.kazik.identity.business.providers.IdentityProvider
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.userIdOf

private const val USER_ROLE = "USER"

@Component
class KeycloakIdentityProvider(
    private val properties: KeycloakAdminProperties,
    private val keycloakRestClient: RestClient,
    private val serviceTokenClient: ServiceTokenClient,
) : IdentityProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun register(account: NewAccount): UserId =
        try {
            val userId = withServiceToken(account) { createUser(account) }
            withServiceToken(account) { assignRole(userId, USER_ROLE) }
            userId
        } catch (exception: ResourceAccessException) {
            throw IdentityProviderUnavailableException(exception)
        } catch (exception: HttpServerErrorException) {
            throw IdentityProviderUnavailableException(exception)
        }

    private fun <T> withServiceToken(account: NewAccount, call: () -> T): T =
        try {
            call()
        } catch (exception: HttpClientErrorException) {
            if (exception.statusCode != HttpStatus.UNAUTHORIZED) throw translate(exception, account)
            serviceTokenClient.invalidate()
            try {
                call()
            } catch (retryException: HttpClientErrorException) {
                throw translate(retryException, account)
            }
        }

    private fun assignRole(userId: UserId, roleName: String) {
        val role = keycloakRestClient.get()
            .uri("/admin/realms/{realm}/roles/{role}", properties.realm, roleName)
            .header("Authorization", "Bearer ${serviceTokenClient.token()}")
            .retrieve()
            .body(Map::class.java)
            ?: error("Keycloak returned no role $roleName")
        keycloakRestClient.post()
            .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", properties.realm, userId.value)
            .header("Authorization", "Bearer ${serviceTokenClient.token()}")
            .contentType(MediaType.APPLICATION_JSON)
            .body(listOf(role))
            .retrieve()
            .toBodilessEntity()
    }

    private fun createUser(account: NewAccount): UserId {
        val response = keycloakRestClient.post()
            .uri("/admin/realms/{realm}/users", properties.realm)
            .header("Authorization", "Bearer ${serviceTokenClient.token()}")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "username" to account.username,
                    "email" to account.email,
                    "enabled" to true,
                    "emailVerified" to true,
                    "credentials" to listOf(mapOf("type" to "password", "value" to account.password, "temporary" to false)),
                ),
            )
            .retrieve()
            .toBodilessEntity()
        val location = response.headers.location ?: error("Keycloak did not return Location of the created user")
        return userIdOf(location.path.substringAfterLast('/'))
    }

    private fun translate(exception: HttpClientErrorException, account: NewAccount): RuntimeException {
        val body = exception.responseBodyAsString
        return when {
            exception.statusCode == HttpStatus.CONFLICT && body.contains("email", ignoreCase = true) -> EmailTakenException()
            exception.statusCode == HttpStatus.CONFLICT -> UsernameTakenException(account.username)
            exception.statusCode == HttpStatus.BAD_REQUEST && body.contains("password", ignoreCase = true) -> WeakPasswordException(extractMessage(body))
            else -> {
                log.error("[IDENTITY] Keycloak rejected registration with {}: {}", exception.statusCode, body)
                IdentityProviderUnavailableException(exception)
            }
        }
    }

    private fun extractMessage(body: String): String =
        Regex("\"error_description\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            ?: Regex("\"errorMessage\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            ?: "password policy is not satisfied"
}
