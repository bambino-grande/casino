package ru.pogosian.kazik

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.exceptions.BusinessRuleException
import ru.pogosian.kazik.common.business.exceptions.ConflictException
import ru.pogosian.kazik.common.business.exceptions.DomainValidationException
import ru.pogosian.kazik.common.business.exceptions.ExternalServiceUnavailableException
import ru.pogosian.kazik.common.business.exceptions.NotFoundException
import ru.pogosian.kazik.common.presentation.ExceptionHandler.Handler
import java.util.UUID

private val HANDLED_EXCEPTIONS = mapOf(
    DomainValidationException::class.java to HttpStatus.BAD_REQUEST,
    NotFoundException::class.java to HttpStatus.NOT_FOUND,
    ConflictException::class.java to HttpStatus.CONFLICT,
    BusinessRuleException::class.java to HttpStatus.UNPROCESSABLE_CONTENT,
    ExternalServiceUnavailableException::class.java to HttpStatus.SERVICE_UNAVAILABLE,
)

class ErrorHandlingIntegrationTest(support: IntegrationTestSupport) : BaseIntegrationTest(support) {
    private fun spinRequest(user: UserId, key: String?, body: String) =
        mockMvc.post("/api/v1/games/slots/capy-classic/spins") {
            with(userJwt(user))
            key?.let { header("Idempotency-Key", it) }
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

    @Test
    fun everyProjectExceptionIsCaughtByTheHandler() {
        val scanner = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean = true
        }
        scanner.addIncludeFilter(AssignableTypeFilter(Throwable::class.java))

        val exceptions = scanner.findCandidateComponents("ru.pogosian.kazik").map { Class.forName(it.beanClassName) }
            .filter { it.name.startsWith("ru.pogosian.kazik") && !it.name.startsWith("ru.pogosian.kazik.common.business.exceptions.DomainException") }

        assertTrue(exceptions.size > 20, "exceptions found: $exceptions")
        val uncaught = exceptions.filter { exception -> HANDLED_EXCEPTIONS.keys.none { it.isAssignableFrom(exception) } }
        assertTrue(uncaught.isEmpty(), "exceptions without a handler: $uncaught")
    }

    @Test
    fun handlerTurnsEveryExceptionFamilyIntoItsStatus() {
        val handler = Handler()
        assertEquals(HttpStatus.BAD_REQUEST.value(), handler.handleDomainValidationException(DomainValidationException("bad", "BAD")).statusCode.value())
        assertEquals(HttpStatus.NOT_FOUND.value(), handler.handleNotFoundException(NotFoundException("missing", "MISSING")).statusCode.value())
        assertEquals(HttpStatus.CONFLICT.value(), handler.handleConflictException(ConflictException("conflict", "CONFLICT")).statusCode.value())
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT.value(), handler.handleBusinessRuleException(BusinessRuleException("rule", "RULE")).statusCode.value())
        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            handler.handleExternalServiceUnavailableException(ExternalServiceUnavailableException("down", "DOWN")).statusCode.value(),
        )
        val unexpected = handler.handleUnexpectedException(IllegalStateException("boom"))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), unexpected.statusCode.value())
        assertEquals("INTERNAL_ERROR", unexpected.body?.code)
    }

    @Test
    fun badRequestsGiveCodes() {
        val user = newUser()
        spinRequest(user, "short", """{"bet": 10}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_IDEMPOTENCY_KEY") }
            jsonPath("$.traceId") { exists() }
        }
        spinRequest(user, key(), "{}").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("MALFORMED_BODY") }
        }
        spinRequest(user, null, """{"bet": 10}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("MISSING_HEADER") }
        }
        mockMvc.get("/api/v1/games/rounds/not-a-uuid") { with(userJwt(user)) }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PARAMETER") }
        }
        mockMvc.get("/api/v1/wallet/transactions?size=0") { with(userJwt(user)) }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PAGE") }
        }
    }

    @Test
    fun notFoundForbiddenAndMethodErrorsGiveCodes() {
        mockMvc.get("/api/v1/games/rounds/${UUID.randomUUID()}") { with(adminJwt(newUser())) }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("ROUND_NOT_FOUND") }
        }
        mockMvc.delete("/api/v1/wallet") { with(userJwt(newUser())) }.andExpect {
            status { isMethodNotAllowed() }
            jsonPath("$.code") { value("METHOD_NOT_ALLOWED") }
        }
        mockMvc.get("/api/v1/nothing-here") { with(userJwt(newUser())) }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("NOT_FOUND") }
        }
        val owner = newUser()
        fund(owner, 100)
        val roundId = json(spinRequest(owner, key(), """{"bet": 10}""").andReturn().response.contentAsString)["roundId"].asString()
        mockMvc.get("/api/v1/games/rounds/$roundId") { with(userJwt(newUser())) }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }
        mockMvc.post("/api/v1/games/slots/unknown-machine/spins") {
            with(userJwt(newUser()))
            header("Idempotency-Key", key())
            contentType = MediaType.APPLICATION_JSON
            content = """{"bet": 10}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("SLOT_MACHINE_NOT_FOUND") }
        }
    }

    @Test
    fun unavailableBankGives503() {
        zBank.stubFor(post(urlEqualTo("/api/partner/payments")).willReturn(aResponse().withStatus(503)))
        mockMvc.post("/api/v1/payments/top-ups") {
            with(userJwt(newUser()))
            header("Idempotency-Key", key())
            contentType = MediaType.APPLICATION_JSON
            content = """{"amount": 500, "card": {"number": "4000000000000002", "expiryMonth": 10, "expiryYear": 2029, "cvv": "007"}}"""
        }.andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.code") { value("PAYMENT_PROVIDER_UNAVAILABLE") }
        }
    }
}
