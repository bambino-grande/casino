package ru.pogosian.kazik.wallet.business.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.wallet.api.AccountRef
import ru.pogosian.kazik.wallet.api.Leg
import ru.pogosian.kazik.wallet.api.SystemAccount
import ru.pogosian.kazik.wallet.api.TransferCommand
import ru.pogosian.kazik.wallet.api.TransferKind
import ru.pogosian.kazik.wallet.business.rates.TopUpRate
import ru.pogosian.kazik.wallet.infrastructure.repository.InboxRepository
import java.time.Clock
import java.util.UUID

private const val TOP_UP_CONSUMER = "wallet.top-up.v1"

@Service
class TopUpCreditServiceImpl(
    private val inboxRepository: InboxRepository,
    private val transferService: TransferService,
    private val topUpRate: TopUpRate,
    private val clock: Clock,
) : TopUpCreditService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun creditTopUp(eventId: UUID, paymentId: UUID, userId: UserId, amount: Rub) {
        if (!inboxRepository.tryMarkProcessed(TOP_UP_CONSUMER, eventId, clock.instant())) {
            log.info("[WALLET] top-up event {} already processed, skip", eventId)
            return
        }
        val naeBalls = topUpRate.toNaeBalls(amount)
        transferService.transfer(
            TransferCommand(
                reference = "payment:$paymentId",
                kind = TransferKind.TOP_UP,
                legs = listOf(
                    Leg(AccountRef.System(SystemAccount.GATEWAY), -naeBalls),
                    Leg(AccountRef.User(userId), naeBalls),
                ),
            ),
        )
        log.info("[WALLET] top-up credited payment={} user={} rub={} naeballs={}", paymentId, userId, amount, naeBalls)
    }
}
