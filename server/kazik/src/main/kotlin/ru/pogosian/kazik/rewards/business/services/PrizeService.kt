package ru.pogosian.kazik.rewards.business.services

import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.rewards.business.prizes.Prize
import ru.pogosian.kazik.rewards.business.providers.StoredImage
import java.util.UUID

interface PrizeService {
    fun grantJackpotPrize(userId: UserId, roundId: UUID): Prize

    fun getUserPrizes(userId: UserId, page: PageQuery): List<Prize>

    fun getPrize(id: UUID): Prize

    fun getImage(id: UUID): StoredImage
}
