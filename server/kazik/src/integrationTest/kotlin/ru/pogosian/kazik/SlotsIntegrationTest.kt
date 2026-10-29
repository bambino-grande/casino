package ru.pogosian.kazik

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.naeBallsOf
import java.math.BigDecimal

class SlotsIntegrationTest(support: IntegrationTestSupport) : BaseIntegrationTest(support) {
    private fun spin(user: UserId, key: String, bet: String) =
        mockMvc.post("/api/v1/games/slots/capy-classic/spins") {
            with(userJwt(user))
            header("Idempotency-Key", key)
            contentType = MediaType.APPLICATION_JSON
            content = """{"bet": $bet}"""
        }

    @Test
    fun spinWithoutTokenIsUnauthorized() {
        mockMvc.post("/api/v1/games/slots/capy-classic/spins") {
            header("Idempotency-Key", key())
            contentType = MediaType.APPLICATION_JSON
            content = """{"bet": 10}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun losingSpinDebitsTheBet() {
        val user = newUser()
        fund(user, 100)
        scriptedRandom.willReturn(7, 13, 18)

        val response = json(spin(user, key(), "10").andExpect { status { isOk() } }.andReturn().response.contentAsString)

        assertTrue(response["prize"].isNull, "приз выдаётся только за джекпот")
        assertEquals(listOf("LEMON", "BELL", "DIAMOND"), (0 until response["reels"].size()).map { response["reels"][it].asString() })
        assertAmount(BigDecimal("90.00"), response["balanceAfter"].decimalValue())
        assertEquals(naeBallsOf(90).minor, balanceOf(user))
    }

    @Test
    fun repeatedRequestWithSameKeyDoesNotChargeTwice() {
        val user = newUser()
        fund(user, 100)
        val key = key()

        spin(user, key, "10").andExpect { status { isOk() } }
        val balanceAfterFirst = balanceOf(user)
        val replay = json(spin(user, key, "10").andExpect { status { isOk() } }.andReturn().response.contentAsString)

        assertTrue(replay["replayed"].asBoolean())
        assertEquals(balanceAfterFirst, balanceOf(user))
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rounds WHERE user_id = ?", Int::class.java, user.value))
    }

    @Test
    fun sameKeyWithAnotherBetIsConflict() {
        val user = newUser()
        fund(user, 100)
        val key = key()
        spin(user, key, "10").andExpect { status { isOk() } }
        spin(user, key, "20").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("IDEMPOTENCY_KEY_REUSED") }
        }
    }

    @Test
    fun notEnoughNaeBallsIsUnprocessableAndLeavesNoRound() {
        val user = newUser()
        fund(user, 5)
        spin(user, key(), "10").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("INSUFFICIENT_FUNDS") }
        }
        assertEquals(naeBallsOf(5).minor, balanceOf(user))
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rounds WHERE user_id = ?", Int::class.java, user.value))
    }

    @Test
    fun invalidBetIsBadRequest() {
        val user = newUser()
        spin(user, key(), "0.001").andExpect { status { isBadRequest() } }
        spin(user, key(), "100000").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_BET") }
        }
        spin(user, "short", "10").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_IDEMPOTENCY_KEY") }
        }
    }

    @Test
    fun threeCapybarasPayTheWholeJackpotAndResetThePool() {
        val user = newUser()
        fund(user, 100)
        val poolBefore = jdbcTemplate.queryForObject("SELECT amount_minor FROM jackpot_pools WHERE code = 'MAIN'", Long::class.java)!!
        scriptedRandom.willReturn(CAPY_ROLL, CAPY_ROLL, CAPY_ROLL)

        val response = json(spin(user, key(), "10").andExpect { status { isOk() } }.andReturn().response.contentAsString)

        val expectedJackpot = poolBefore + 20
        assertEquals("CAPYBARA_RAIN", response["animation"].asString())
        assertEquals("/api/v1/rewards/prizes/${response["prize"]["prizeId"].asString()}/image", response["prize"]["imageUrl"].asString())
        assertAmount(BigDecimal.valueOf(expectedJackpot, 2), response["jackpotWin"].decimalValue())
        assertEquals(naeBallsOf(90).minor + expectedJackpot, balanceOf(user))
        assertEquals(100_000L, jdbcTemplate.queryForObject("SELECT amount_minor FROM jackpot_pools WHERE code = 'MAIN'", Long::class.java))

        mockMvc.get("/api/v1/games/jackpot") { with(userJwt(user)) }.andExpect {
            status { isOk() }
            jsonPath("$.recentWins[0].amount") { value(BigDecimal.valueOf(expectedJackpot, 2).toDouble()) }
            jsonPath("$.recentWins[0].userId") { doesNotExist() }
        }
    }

    @Test
    fun userCannotSeeSomeoneElsesRound() {
        val owner = newUser()
        fund(owner, 100)
        val roundId = json(spin(owner, key(), "10").andReturn().response.contentAsString)["roundId"].asString()

        mockMvc.get("/api/v1/games/rounds/$roundId") { with(userJwt(newUser())) }.andExpect { status { isForbidden() } }
        mockMvc.get("/api/v1/games/rounds/$roundId") { with(userJwt(owner)) }.andExpect { status { isOk() } }
        mockMvc.get("/api/v1/games/rounds/$roundId") { with(adminJwt(newUser())) }.andExpect { status { isOk() } }
    }
}
