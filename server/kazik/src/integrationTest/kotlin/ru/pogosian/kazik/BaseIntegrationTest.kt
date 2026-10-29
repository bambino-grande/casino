package ru.pogosian.kazik

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.naeBallsOf
import ru.pogosian.kazik.wallet.api.AccountRef
import ru.pogosian.kazik.wallet.api.Leg
import ru.pogosian.kazik.wallet.api.SystemAccount
import ru.pogosian.kazik.wallet.api.TransferCommand
import ru.pogosian.kazik.wallet.api.TransferKind
import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration::class)
abstract class BaseIntegrationTest(support: IntegrationTestSupport) {
    protected val mockMvc = support.mockMvc
    protected val objectMapper = support.objectMapper
    protected val transferService = support.transferService
    protected val jdbcTemplate = support.jdbcTemplate
    protected val scriptedRandom = support.scriptedRandom
    protected val zBank = support.zBank
    protected val prizeImagesDir = support.prizeImages.path

    @BeforeEach
    fun resetFakes() {
        scriptedRandom.reset()
        zBank.resetAll()
    }

    protected fun newUser(): UserId = UserId(UUID.randomUUID())

    protected fun userJwt(userId: UserId): JwtRequestPostProcessor = jwtWithRoles(userId, "USER")

    protected fun adminJwt(userId: UserId): JwtRequestPostProcessor = jwtWithRoles(userId, "ADMIN")

    private fun jwtWithRoles(userId: UserId, vararg roles: String): JwtRequestPostProcessor =
        jwt()
            .jwt { token ->
                token.subject(userId.toString())
                    .claim("preferred_username", "user-${userId.toString().take(8)}")
                    .claim("realm_access", mapOf("roles" to roles.toList()))
            }
            .authorities(roles.map { SimpleGrantedAuthority("ROLE_$it") })

    protected fun fund(userId: UserId, amount: Long) {
        transferService.transfer(
            TransferCommand(
                reference = "test-funding:${UUID.randomUUID()}",
                kind = TransferKind.TOP_UP,
                legs = listOf(Leg(AccountRef.System(SystemAccount.GATEWAY), naeBallsOf(-amount)), Leg(AccountRef.User(userId), naeBallsOf(amount))),
            ),
        )
    }

    protected fun balanceOf(userId: UserId): Long =
        jdbcTemplate.queryForList("SELECT balance_minor FROM accounts WHERE owner_id = ?", Long::class.java, userId.value)
            .singleOrNull() ?: 0L

    protected fun json(body: String): JsonNode = objectMapper.readTree(body)

    protected fun assertAmount(expected: BigDecimal, actual: BigDecimal) {
        assertEquals(0, expected.compareTo(actual), "expected $expected but was $actual")
    }

    protected fun key(): String = "test-" + UUID.randomUUID().toString().take(18)
}
