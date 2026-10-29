package ru.pogosian.kazik

import java.nio.file.Files
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.rewards.business.exceptions.PrizeOwnerMismatchException
import ru.pogosian.kazik.rewards.business.services.PrizeService
import tools.jackson.databind.JsonNode

class RewardsIntegrationTest(
    support: IntegrationTestSupport,
    private val prizeService: PrizeService,
) : BaseIntegrationTest(support) {
    private fun spin(user: UserId, key: String) =
        mockMvc.post("/api/v1/games/slots/capy-classic/spins") {
            with(userJwt(user))
            header("Idempotency-Key", key)
            contentType = MediaType.APPLICATION_JSON
            content = """{"bet": 10}"""
        }

    private fun jackpotSpin(user: UserId, key: String): JsonNode {
        scriptedRandom.willReturn(CAPY_ROLL, CAPY_ROLL, CAPY_ROLL)
        return json(spin(user, key).andExpect { status { isOk() } }.andReturn().response.contentAsString)
    }

    private fun prizeCount(roundId: String): Long =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prizes WHERE round_id = ?::uuid", Long::class.java, roundId)!!

    @Test
    fun jackpotGivesPrizeAndItAppearsInTheCollection() {
        val user = newUser()
        fund(user, 100)

        val response = jackpotSpin(user, key())
        val prizeId = response["prize"]["prizeId"].asString()

        assertEquals("CAPYBARA_RAIN", response["animation"].asString())
        assertEquals("/api/v1/rewards/prizes/$prizeId/image", response["prize"]["imageUrl"].asString())
        assertEquals(1L, prizeCount(response["roundId"].asString()))
        mockMvc.get("/api/v1/rewards/prizes") { with(userJwt(user)) }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].prizeId") { value(prizeId) }
            jsonPath("$[0].roundId") { value(response["roundId"].asString()) }
            jsonPath("$[0].imageUrl") { value("/api/v1/rewards/prizes/$prizeId/image") }
        }
    }

    @Test
    fun spinWithoutJackpotGivesNoPrize() {
        val user = newUser()
        fund(user, 100)
        scriptedRandom.willReturn(7, 13, 18)

        val response = json(spin(user, key()).andExpect { status { isOk() } }.andReturn().response.contentAsString)

        assertTrue(response["prize"].isNull)
        mockMvc.get("/api/v1/rewards/prizes") { with(userJwt(user)) }.andExpect { jsonPath("$.length()") { value(0) } }
    }

    @Test
    fun repeatedRequestWithSameKeyReturnsTheSamePrizeAndDoesNotDuplicateIt() {
        val user = newUser()
        fund(user, 100)
        val key = key()

        val first = jackpotSpin(user, key)
        val replay = json(spin(user, key).andExpect { status { isOk() } }.andReturn().response.contentAsString)

        assertTrue(replay["replayed"].asBoolean())
        assertEquals(first["prize"]["prizeId"].asString(), replay["prize"]["prizeId"].asString())
        assertEquals(1L, prizeCount(first["roundId"].asString()))
    }

    @Test
    fun prizeIsVisibleOnlyToItsOwnerAndAdmin() {
        val owner = newUser()
        fund(owner, 100)
        val prizeId = jackpotSpin(owner, key())["prize"]["prizeId"].asString()

        mockMvc.get("/api/v1/rewards/prizes/$prizeId") { with(userJwt(owner)) }.andExpect { status { isOk() } }
        mockMvc.get("/api/v1/rewards/prizes/$prizeId") { with(userJwt(newUser())) }.andExpect { status { isForbidden() } }
        mockMvc.get("/api/v1/rewards/prizes/$prizeId") { with(adminJwt(newUser())) }.andExpect { status { isOk() } }
        mockMvc.get("/api/v1/rewards/prizes/${UUID.randomUUID()}") { with(adminJwt(newUser())) }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("PRIZE_NOT_FOUND") }
        }
        mockMvc.get("/api/v1/rewards/prizes").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun grantingIsIdempotentPerRoundAndBoundToItsOwner() {
        val user = newUser()
        val roundId = UUID.randomUUID()

        val first = prizeService.grantJackpotPrize(user, roundId)
        val second = prizeService.grantJackpotPrize(user, roundId)

        assertEquals(first.id, second.id)
        assertEquals(1L, prizeCount(roundId.toString()))
        assertThrows<PrizeOwnerMismatchException> { prizeService.grantJackpotPrize(newUser(), roundId) }
    }

    @Test
    fun imageIsTheFileFromTheVolumeAndIsGivenOnlyToItsOwner() {
        val owner = newUser()
        fund(owner, 100)
        val prizeId = jackpotSpin(owner, key())["prize"]["prizeId"].asString()
        val fileName = jdbcTemplate.queryForObject("SELECT image_file FROM prizes WHERE id = ?::uuid", String::class.java, prizeId)!!

        mockMvc.get("/api/v1/rewards/prizes/$prizeId/image") { with(userJwt(owner)) }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith("image/png") }
            content { bytes(Files.readAllBytes(prizeImagesDir.resolve(fileName))) }
        }
        mockMvc.get("/api/v1/rewards/prizes/$prizeId/image") { with(userJwt(newUser())) }.andExpect { status { isForbidden() } }
        mockMvc.get("/api/v1/rewards/prizes/$prizeId/image").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun prizesAreRandomImagesFromTheVolumeAndNeverOtherFiles() {
        repeat(30) { prizeService.grantJackpotPrize(newUser(), UUID.randomUUID()) }

        val awarded = jdbcTemplate.queryForList("SELECT DISTINCT image_file FROM prizes", String::class.java).toSet()

        assertEquals(setOf("prize-a.png", "prize-b.png"), awarded)
    }
}
