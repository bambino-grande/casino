package ru.pogosian.kazik.games.business.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.games.business.exceptions.RoundNotFoundException
import ru.pogosian.kazik.games.business.rounds.Round
import ru.pogosian.kazik.games.business.slots.SlotMachine
import ru.pogosian.kazik.games.business.slots.SlotMachineCatalog
import ru.pogosian.kazik.games.infrastructure.repository.JackpotPoolRepository
import ru.pogosian.kazik.games.infrastructure.repository.RoundRepository
import java.util.UUID

private const val RECENT_JACKPOTS = 10

@Service
class RoundServiceImpl(
    private val catalog: SlotMachineCatalog,
    private val roundRepository: RoundRepository,
    private val jackpotPoolRepository: JackpotPoolRepository,
) : RoundService {
    override fun getMachines(): List<SlotMachine> = catalog.all()

    @Transactional(readOnly = true)
    override fun getRound(id: UUID): Round = roundRepository.findById(id) ?: throw RoundNotFoundException(id)

    @Transactional(readOnly = true)
    override fun getUserRounds(userId: UserId, page: PageQuery): List<Round> = roundRepository.findByUser(userId, page)

    @Transactional(readOnly = true)
    override fun getJackpot(): JackpotInfo = JackpotInfo(jackpotPoolRepository.findMain(), roundRepository.findRecentJackpots(RECENT_JACKPOTS))
}
