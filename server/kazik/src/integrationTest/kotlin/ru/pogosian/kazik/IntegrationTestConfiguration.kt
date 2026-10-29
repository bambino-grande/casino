package ru.pogosian.kazik

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistrar
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.postgresql.PostgreSQLContainer
import ru.pogosian.kazik.games.business.providers.RandomSource
import ru.pogosian.kazik.wallet.business.services.TransferService
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Base64

const val CAPY_ROLL = 23

private const val ONE_PIXEL_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="

class ScriptedRandomSource : RandomSource {
    private val script = ArrayDeque<Int>()
    private val fallback = SecureRandom()

    fun willReturn(vararg rolls: Int) {
        script.addAll(rolls.toList())
    }

    fun reset() = script.clear()

    override fun nextInt(bound: Int): Int = script.removeFirstOrNull() ?: fallback.nextInt(bound)
}

class PrizeImagesDirectory(val path: Path)

class IntegrationTestSupport(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val transferService: TransferService,
    val jdbcTemplate: JdbcTemplate,
    val scriptedRandom: ScriptedRandomSource,
    val zBank: WireMockServer,
    val prizeImages: PrizeImagesDirectory,
)

@TestConfiguration(proxyBeanMethods = false)
class IntegrationTestConfiguration {
    @Bean
    @ServiceConnection
    fun postgres(): PostgreSQLContainer = PostgreSQLContainer("postgres:18")

    @Bean(destroyMethod = "stop")
    fun zBank(): WireMockServer = WireMockServer(wireMockConfig().dynamicPort()).apply { start() }

    @Bean
    fun prizeImagesDirectory(): PrizeImagesDirectory {
        val dir = Files.createTempDirectory("kazik-prizes")
        val png = Base64.getDecoder().decode(ONE_PIXEL_PNG)
        Files.write(dir.resolve("prize-a.png"), png)
        Files.write(dir.resolve("prize-b.png"), png)
        Files.writeString(dir.resolve("notes.txt"), "это не картинка")
        return PrizeImagesDirectory(dir)
    }

    @Bean
    fun testProperties(zBank: WireMockServer, prizeImages: PrizeImagesDirectory): DynamicPropertyRegistrar =
        DynamicPropertyRegistrar { registry ->
            registry.add("kazik.payments.zbank.base-url") { zBank.baseUrl() }
            registry.add("kazik.rewards.images-dir") { prizeImages.path.toString() }
        }

    @Bean
    fun jwtDecoder(): JwtDecoder = JwtDecoder { throw BadJwtException("tests authenticate with mock JWTs") }

    @Bean
    @Primary
    fun scriptedRandomSource(): ScriptedRandomSource = ScriptedRandomSource()

    @Bean
    fun integrationTestSupport(
        mockMvc: MockMvc,
        objectMapper: ObjectMapper,
        transferService: TransferService,
        jdbcTemplate: JdbcTemplate,
        scriptedRandom: ScriptedRandomSource,
        zBank: WireMockServer,
        prizeImages: PrizeImagesDirectory,
    ): IntegrationTestSupport = IntegrationTestSupport(mockMvc, objectMapper, transferService, jdbcTemplate, scriptedRandom, zBank, prizeImages)
}
