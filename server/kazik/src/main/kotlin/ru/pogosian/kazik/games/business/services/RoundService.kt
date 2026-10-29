package ru.pogosian.kazik.games.business.services

import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.games.business.jackpot.JackpotPool
import ru.pogosian.kazik.games.business.rounds.Round
import ru.pogosian.kazik.games.business.slots.SlotMachine
import java.util.UUID

data class JackpotInfo(
    val pool: JackpotPool,
    val recentWins: List<Round>,
)

interface RoundService {
    fun getMachines(): List<SlotMachine>

    fun getRound(id: UUID): Round

    fun getUserRounds(userId: UserId, page: PageQuery): List<Round>

    fun getJackpot(): JackpotInfo
}
