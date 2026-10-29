package ru.pogosian.kazik.rewards.business.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.uuidV7
import ru.pogosian.kazik.rewards.business.exceptions.PrizeNotFoundException
import ru.pogosian.kazik.rewards.business.exceptions.PrizeOwnerMismatchException
import ru.pogosian.kazik.rewards.business.prizes.Prize
import ru.pogosian.kazik.rewards.business.providers.PrizeImageProvider
import ru.pogosian.kazik.rewards.business.providers.PrizeImageStorage
import ru.pogosian.kazik.rewards.business.providers.StoredImage
import ru.pogosian.kazik.rewards.infrastructure.repository.PrizeRepository
import java.time.Clock
import java.util.UUID

@Service
class PrizeServiceImpl(
    private val prizeRepository: PrizeRepository,
    private val prizeImageProvider: PrizeImageProvider,
    private val prizeImageStorage: PrizeImageStorage,
    private val clock: Clock,
) : PrizeService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun grantJackpotPrize(userId: UserId, roundId: UUID): Prize {
        prizeRepository.findByRound(roundId)?.let { existing ->
            if (existing.userId != userId) throw PrizeOwnerMismatchException(roundId)
            return existing
        }
        val now = clock.instant()
        val prize = Prize(uuidV7(now), userId, roundId, prizeImageProvider.pick().fileName, now)
        prizeRepository.insert(prize)
        log.info("[REWARDS] prize {} granted to user {} for jackpot round {}", prize.id, userId, roundId)
        return prize
    }

    @Transactional(readOnly = true)
    override fun getUserPrizes(userId: UserId, page: PageQuery): List<Prize> = prizeRepository.findByUser(userId, page)

    @Transactional(readOnly = true)
    override fun getPrize(id: UUID): Prize = prizeRepository.findById(id) ?: throw PrizeNotFoundException(id)

    @Transactional(readOnly = true)
    override fun getImage(id: UUID): StoredImage = prizeImageStorage.read(getPrize(id).imageFile)
}
