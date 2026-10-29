package ru.pogosian.kazik

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.naeBallsOf
import ru.pogosian.kazik.payments.business.services.PaymentExpiryService
import ru.pogosian.kazik.payments.infrastructure.messaging.OutboxRelay
import java.util.UUID

private const val MERCHANT_ACCOUNT = "22222222-2222-4222-8222-222222222222"

class PaymentsIntegrationTest(
    support: IntegrationTestSupport,
    private val outboxRelay: OutboxRelay,
    private val paymentExpiryService: PaymentExpiryService,
) : BaseIntegrationTest(support) {
    private fun operation(status: String, errorCode: String?) =
        """{"id":"${UUID.randomUUID()}","kind":"PAYMENT","status":"$status","amountKopecks":50000,
           "sourceAccountId":null,"destinationAccountId":${if (status == "SUCCEEDED") "\"$MERCHANT_ACCOUNT\"" else "null"},
           "itemId":null,"errorCode":${errorCode?.let { "\"$it\"" }},"createdAt":"2026-10-28T12:00:00Z"}"""

    private fun stubBank(httpStatus: Int, body: String) {
        zBank.stubFor(
            post(urlEqualTo("/api/partner/payments"))
                .willReturn(aResponse().withStatus(httpStatus).withHeader("Content-Type", "application/json").withBody(body)),
        )
    }

    private fun createTopUp(user: UserId, key: String, amount: Int, cvv: String) =
        mockMvc.post("/api/v1/payments/top-ups") {
            with(userJwt(user))
            header("Idempotency-Key", key)
            contentType = MediaType.APPLICATION_JSON
            content = """{"amount": $amount, "card": {"number": "4000000000000002", "expiryMonth": 10, "expiryYear": 2029, "cvv": "$cvv"}}"""
        }

    private fun createTopUp(user: UserId, key: String) = createTopUp(user, key, 500, "007")

    private fun donate(user: UserId, key: String, body: String) =
        mockMvc.post("/api/v1/donations") {
            with(userJwt(user))
            header("Idempotency-Key", key)
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

    private fun paymentIdOf(user: UserId, key: String): String =
        jdbcTemplate.queryForObject("SELECT id::text FROM payments WHERE user_id = ? AND idempotency_key = ?", String::class.java, user.value, key)!!

    private fun paymentStatus(paymentId: String): String =
        jdbcTemplate.queryForObject("SELECT status FROM payments WHERE id = ?::uuid", String::class.java, paymentId)!!

    @Test
    fun topUpWithCardIsChargedSynchronouslyAndCreditsNaeBallsOneToOne() {
        val user = newUser()
        stubBank(200, operation("SUCCEEDED", null))

        createTopUp(user, key()).andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("SUCCEEDED") }
            jsonPath("$.sbpTransfer") { doesNotExist() }
        }
        outboxRelay.publishPending()
        outboxRelay.publishPending()

        assertEquals(naeBallsOf(500).minor, balanceOf(user))
    }

    @Test
    fun cardDetailsAreNeverStored() {
        val user = newUser()
        stubBank(200, operation("SUCCEEDED", null))
        createTopUp(user, key()).andExpect { status { isCreated() } }

        val leaked = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM payments p WHERE p::text LIKE '%4000000000000002%' OR p::text LIKE '%\"007\"%'",
            Long::class.java,
        )
        assertEquals(0L, leaked)
    }

    @Test
    fun repeatedTopUpWithSameKeyDoesNotChargeTwice() {
        val user = newUser()
        stubBank(200, operation("SUCCEEDED", null))
        val key = key()

        val first = json(createTopUp(user, key).andReturn().response.contentAsString)["id"].asString()
        val second = json(createTopUp(user, key).andReturn().response.contentAsString)["id"].asString()

        assertEquals(first, second)
        zBank.verify(1, postRequestedFor(urlEqualTo("/api/partner/payments")))
        createTopUp(user, key, 700, "007").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("IDEMPOTENCY_KEY_REUSED") }
        }
    }

    @Test
    fun declinedCardGives422AndNoNaeBalls() {
        val user = newUser()
        stubBank(422, operation("DECLINED", "INSUFFICIENT_FUNDS"))
        val key = key()

        createTopUp(user, key).andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("CARD_INSUFFICIENT_FUNDS") }
        }
        createTopUp(user, key).andExpect { status { isUnprocessableContent() } }
        outboxRelay.publishPending()

        zBank.verify(1, postRequestedFor(urlEqualTo("/api/partner/payments")))
        assertEquals(0L, balanceOf(user))
    }

    @Test
    fun unknownResultGives503AndRetryWithSameKeyAndCardFinishesThePayment() {
        val user = newUser()
        val key = key()
        stubBank(502, "<html>Bad Gateway</html>")
        createTopUp(user, key).andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.code") { value("PAYMENT_PROVIDER_UNAVAILABLE") }
        }
        val paymentId = paymentIdOf(user, key)
        assertEquals("CREATED", paymentStatus(paymentId))

        stubBank(200, operation("SUCCEEDED", null))
        createTopUp(user, key).andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("SUCCEEDED") }
        }

        val bankKey = "kazik-topup:" + jdbcTemplate.queryForObject("SELECT operation_id::text FROM payments WHERE id = ?::uuid", String::class.java, paymentId)
        zBank.verify(2, postRequestedFor(urlEqualTo("/api/partner/payments")).withHeader("Idempotency-Key", equalTo(bankKey)))
    }

    @Test
    fun unpaidTopUpExpiresAndCanStillBeFinishedWithTheSameKey() {
        val user = newUser()
        val key = key()
        stubBank(502, "<html>Bad Gateway</html>")
        createTopUp(user, key).andExpect { status { isServiceUnavailable() } }
        val paymentId = paymentIdOf(user, key)
        jdbcTemplate.update("UPDATE payments SET expires_at = now() - interval '1 minute' WHERE id = ?::uuid", paymentId)

        assertTrue(paymentExpiryService.expireOverdue() >= 1)
        assertEquals("EXPIRED", paymentStatus(paymentId))

        stubBank(200, operation("SUCCEEDED", null))
        createTopUp(user, key).andExpect { jsonPath("$.status") { value("SUCCEEDED") } }
        outboxRelay.publishPending()
        assertEquals(naeBallsOf(500).minor, balanceOf(user))
    }

    @Test
    fun topUpWithoutCardOrWithBadCvvIsBadRequest() {
        mockMvc.post("/api/v1/payments/top-ups") {
            with(userJwt(newUser()))
            header("Idempotency-Key", key())
            contentType = MediaType.APPLICATION_JSON
            content = """{"amount": 500}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("MALFORMED_BODY") }
        }
        createTopUp(newUser(), key(), 500, "7").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_CARD") }
        }
        zBank.verify(0, postRequestedFor(urlEqualTo("/api/partner/payments")))
    }

    @Test
    fun topUpAmountOutsideLimitsIsBadRequest() {
        createTopUp(newUser(), key(), 10, "007").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_PAYMENT_AMOUNT") }
        }
    }

    @Test
    fun donationIsAcceptedImmediatelyAppearsOnTheWallAndNeverTouchesTheWallet() {
        val donor = newUser()
        val walletRowsBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_entries", Long::class.java)
        val key = key()
        val body = """{"amount": 300, "donorName": "Капибарофил", "message": "за капибар", "showOnWall": true}"""

        val donation = json(donate(donor, key, body).andExpect { status { isCreated() } }.andReturn().response.contentAsString)
        val paymentId = donation["id"].asString()
        assertEquals("SUCCEEDED", donation["status"].asString())
        assertTrue(donation["sbpTransfer"]["comment"].asString().startsWith("KZK-"))

        outboxRelay.publishPending()

        assertEquals(walletRowsBefore, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_entries", Long::class.java))
        mockMvc.get("/api/v1/donations").andExpect {
            status { isOk() }
            jsonPath("$[?(@.paymentId == '$paymentId')].donorName") { value("Капибарофил") }
        }
        donate(donor, key, body).andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(paymentId) }
        }
    }

    @Test
    fun donationHiddenFromTheWallStaysOffIt() {
        val body = """{"amount": 150, "donorName": "Тайный", "showOnWall": false}"""
        val paymentId = json(donate(newUser(), key(), body).andExpect { status { isCreated() } }.andReturn().response.contentAsString)["id"].asString()

        mockMvc.get("/api/v1/donations").andExpect {
            status { isOk() }
            jsonPath("$[?(@.paymentId == '$paymentId')]") { isEmpty() }
        }
    }

    @Test
    fun twoDonorsWithTheSameKeyGetTheirOwnDonations() {
        val sharedKey = "donate-0001-a1b2"
        val first = json(donate(newUser(), sharedKey, """{"amount": 100, "donorName": "Первый", "showOnWall": true}""").andReturn().response.contentAsString)
        val second = json(donate(newUser(), sharedKey, """{"amount": 100, "donorName": "Второй", "showOnWall": true}""").andReturn().response.contentAsString)

        assertTrue(first["id"].asString() != second["id"].asString())
        assertEquals("Второй", second["donation"]["donorName"].asString())
    }

    @Test
    fun donationRequiresToken() {
        mockMvc.post("/api/v1/donations") {
            header("Idempotency-Key", key())
            contentType = MediaType.APPLICATION_JSON
            content = """{"amount": 100, "showOnWall": true}"""
        }.andExpect { status { isUnauthorized() } }
    }
}
