package ru.pogosian.kazik.games.business.providers

import ru.pogosian.kazik.common.business.UserId
import java.util.UUID

interface RewardsGateway {
    fun grantJackpotPrize(userId: UserId, roundId: UUID): GrantedPrize?
}

data class GrantedPrize(
    val id: UUID,
    val imageUrl: String,
)
