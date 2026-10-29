package ru.pogosian.kazik.wallet.business.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.ZERO_NAE_BALLS
import ru.pogosian.kazik.wallet.business.ledger.LedgerEntryView
import ru.pogosian.kazik.wallet.infrastructure.repository.AccountRepository
import ru.pogosian.kazik.wallet.infrastructure.repository.LedgerRepository

@Service
class WalletServiceImpl(
    private val accountRepository: AccountRepository,
    private val ledgerRepository: LedgerRepository,
) : WalletService {
    @Transactional(readOnly = true)
    override fun getBalance(userId: UserId): NaeBalls = accountRepository.findUserAccount(userId)?.balance ?: ZERO_NAE_BALLS

    @Transactional(readOnly = true)
    override fun getHistory(userId: UserId, page: PageQuery): List<LedgerEntryView> {
        val account = accountRepository.findUserAccount(userId) ?: return emptyList()
        return ledgerRepository.findHistory(account.id, page)
    }
}
