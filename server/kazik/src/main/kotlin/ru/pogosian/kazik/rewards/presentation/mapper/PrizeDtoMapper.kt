package ru.pogosian.kazik.rewards.presentation.mapper

import org.springframework.stereotype.Component
import ru.pogosian.kazik.rewards.api.prizeImagePath
import ru.pogosian.kazik.rewards.business.prizes.Prize
import ru.pogosian.kazik.rewards.presentation.DTO.response.PrizeResponse

@Component
class PrizeDtoMapper {
    fun toDto(prize: Prize): PrizeResponse = PrizeResponse(prize.id, prize.roundId, prizeImagePath(prize.id), prize.grantedAt)
}
