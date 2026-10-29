package ru.pogosian.kazik.payments.infrastructure.providers.zbank

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import ru.pogosian.kazik.payments.business.exceptions.PaymentInstrumentRequiredException
import ru.pogosian.kazik.payments.business.exceptions.PaymentProviderUnavailableException
import ru.pogosian.kazik.payments.business.exceptions.ProviderIdempotencyConflictException
import ru.pogosian.kazik.payments.business.payments.PaymentMethod
import ru.pogosian.kazik.payments.business.providers.PaymentProvider
import ru.pogosian.kazik.payments.business.providers.ProviderPayment
import ru.pogosian.kazik.payments.business.providers.ProviderPaymentRequest
import ru.pogosian.kazik.payments.business.providers.ProviderPaymentStatus
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.http.HttpClient

@Component
class ZBankPaymentProvider(
    private val properties: ZBankProperties,
    private val objectMapper: ObjectMapper,
    restClientBuilder: RestClient.Builder,
) : PaymentProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient = restClientBuilder
        .baseUrl(properties.baseUrl.trimEnd('/'))
        .defaultHeader("Authorization", "Bearer ${properties.apiKey}")
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(properties.connectTimeout)
                    .build(),
            ).apply { setReadTimeout(properties.readTimeout) },
        )
        .build()

    override val method: PaymentMethod = PaymentMethod.Z_BANK

    override fun create(request: ProviderPaymentRequest): ProviderPayment {
        val card = request.card ?: throw PaymentInstrumentRequiredException(method)
        val body = ZBankPaymentRequest(card.number, card.expiryMonth, card.expiryYear, card.cvv, request.amount.kopecks)
        val (status, json) = call {
            restClient.post()
                .uri("/api/partner/payments")
                .header("Idempotency-Key", "kazik-topup:${request.operationId}")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
        }
        val operation = operationOrNull(json) ?: handleError(status, json)

        if (operation.kind != "PAYMENT" || operation.amountKopecks != request.amount.kopecks) {
            log.error("[ZBANK] unexpected operation {}: kind={} amount={}", operation.id, operation.kind, operation.amountKopecks)
            throw PaymentProviderUnavailableException(method, null)
        }
        return when (operation.status) {
            "SUCCEEDED" -> {
                if (operation.errorCode != null || operation.destinationAccountId != properties.accountId) {
                    log.error("[ZBANK] operation {} succeeded with errorCode={} to account {}", operation.id, operation.errorCode, operation.destinationAccountId)
                    throw PaymentProviderUnavailableException(method, null)
                }
                log.info("[ZBANK] payment {} succeeded from card {} for {} kopecks", operation.id, card.masked, operation.amountKopecks)
                ProviderPayment(operation.id, ProviderPaymentStatus.SUCCEEDED, null, null)
            }
            "DECLINED" -> {
                log.info("[ZBANK] payment {} declined: {} (card {})", operation.id, operation.errorCode, card.masked)
                ProviderPayment(operation.id, ProviderPaymentStatus.DECLINED, operation.errorCode ?: "DECLINED", null)
            }
            else -> {
                log.error("[ZBANK] operation {} has unknown status {}", operation.id, operation.status)
                throw PaymentProviderUnavailableException(method, null)
            }
        }
    }

    private fun call(request: () -> RestClient.RequestHeadersSpec<*>): Pair<Int, JsonNode?> =
        try {
            request().exchange { _, response ->
                val status = response.statusCode.value()
                val bytes = response.body.readAllBytes()
                if (status >= 500) {
                    log.warn("[ZBANK] payment failed with HTTP {}", status)
                    throw PaymentProviderUnavailableException(method, null)
                }
                status to runCatching { objectMapper.readTree(bytes) }.getOrNull()
            }
        } catch (exception: ResourceAccessException) {
            log.warn("[ZBANK] payment failed: {}", exception.message)
            throw PaymentProviderUnavailableException(method, exception)
        }

    private fun operationOrNull(json: JsonNode?): ZBankOperation? {
        if (json == null || !json.isObject || !json.has("id") || !json.has("status") || !json.has("kind")) return null
        return runCatching { objectMapper.treeToValue(json, ZBankOperation::class.java) }.getOrNull()
    }

    private fun handleError(status: Int, json: JsonNode?): Nothing {
        val error = json?.takeIf { it.isObject && it.has("code") }?.let { runCatching { objectMapper.treeToValue(it, ZBankError::class.java) }.getOrNull() }
        when (error?.code) {
            "CONFLICT" -> throw ProviderIdempotencyConflictException(method)
            "UNAUTHORIZED", "FORBIDDEN" -> log.error("[ZBANK] payment rejected with {}: check ZBANK_API_KEY", error.code)
            null -> log.warn("[ZBANK] payment returned HTTP {} with unexpected body", status)
            else -> log.warn("[ZBANK] payment returned error {} (HTTP {})", error.code, status)
        }
        throw PaymentProviderUnavailableException(method, null)
    }
}
