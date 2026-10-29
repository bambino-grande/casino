package ru.pogosian.kazik.games.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.pogosian.kazik.common.business.naeBallsOf
import ru.pogosian.kazik.games.business.jackpot.JackpotPolicy
import ru.pogosian.kazik.games.business.slots.SlotMachine
import ru.pogosian.kazik.games.business.slots.SlotMachineCatalog
import ru.pogosian.kazik.games.business.slots.Symbol

@ConfigurationProperties(prefix = "kazik.games")
data class GamesProperties(
    val jackpotContributionBasisPoints: Int,
)

@Configuration
@EnableConfigurationProperties(GamesProperties::class)
class GamesConfiguration {
    @Bean
    fun slotMachineCatalog(): SlotMachineCatalog =
        SlotMachineCatalog(
            listOf(
                SlotMachine(
                    code = "capy-classic",
                    title = "Капибара Классик",
                    paytableVersion = 1,
                    minBet = naeBallsOf(1),
                    maxBet = naeBallsOf(500),
                    reelWeights = mapOf(
                        Symbol.CHERRY to 7,
                        Symbol.LEMON to 6,
                        Symbol.BELL to 5,
                        Symbol.DIAMOND to 3,
                        Symbol.SEVEN to 2,
                        Symbol.CAPY to 1,
                    ),
                    threeOfKind = mapOf(
                        Symbol.CHERRY to 6,
                        Symbol.LEMON to 8,
                        Symbol.BELL to 12,
                        Symbol.DIAMOND to 40,
                        Symbol.SEVEN to 100,
                    ),
                    twoCherries = 2,
                    twoCapybaras = 10,
                    bigWinMultiplier = 40,
                ),
            ),
        )

    @Bean
    fun jackpotPolicy(properties: GamesProperties): JackpotPolicy = JackpotPolicy(properties.jackpotContributionBasisPoints)
}
