package ru.pogosian.kazik.games.infrastructure.rewards

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.games.business.providers.GrantedPrize
import ru.pogosian.kazik.games.business.providers.RewardsGateway
import ru.pogosian.kazik.rewards.api.prizeImagePath
import ru.pogosian.kazik.rewards.business.exceptions.NoPrizeImagesException
import ru.pogosian.kazik.rewards.business.services.PrizeService
import java.util.UUID

@Component
class PrizeServiceGateway(
    private val prizeService: PrizeService,
) : RewardsGateway {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun grantJackpotPrize(userId: UserId, roundId: UUID): GrantedPrize? =
        try {
            prizeService.grantJackpotPrize(userId, roundId).let { GrantedPrize(it.id, prizeImagePath(it.id)) }
        } catch (exception: NoPrizeImagesException) {
            log.error("[GAMES] prize for jackpot round {} was not granted: {}", roundId, exception.message)
            null
        }
}
