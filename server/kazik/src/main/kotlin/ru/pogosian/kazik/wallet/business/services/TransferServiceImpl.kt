package ru.pogosian.kazik.wallet.business.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.pogosian.kazik.common.business.uuidV7
import ru.pogosian.kazik.wallet.api.AccountRef
import ru.pogosian.kazik.wallet.api.TransferCommand
import ru.pogosian.kazik.wallet.api.TransferResult
import ru.pogosian.kazik.wallet.business.accounts.Account
import ru.pogosian.kazik.wallet.business.accounts.AccountOwner
import ru.pogosian.kazik.wallet.business.exceptions.InsufficientFundsException
import ru.pogosian.kazik.wallet.business.ledger.LedgerEntry
import ru.pogosian.kazik.wallet.business.ledger.LedgerTransaction
import ru.pogosian.kazik.wallet.infrastructure.repository.AccountRepository
import ru.pogosian.kazik.wallet.infrastructure.repository.LedgerRepository
import java.time.Clock

@Service
class TransferServiceImpl(
    private val accountRepository: AccountRepository,
    private val ledgerRepository: LedgerRepository,
    private val clock: Clock,
) : TransferService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun transfer(command: TransferCommand): TransferResult {
        val now = clock.instant()
        val accounts = command.legs.map { resolveAccount(it.account) }
        val transaction = LedgerTransaction(
            id = uuidV7(now),
            reference = command.reference,
            kind = command.kind,
            entries = command.legs.zip(accounts) { leg, account -> LedgerEntry(uuidV7(now), account.id, leg.amount) },
            createdAt = now,
        )

        ledgerRepository.insertTransaction(transaction)
        transaction.entries.sortedBy { it.accountId }.forEach { entry ->
            if (!accountRepository.applyDelta(entry.accountId, entry.amount)) {
                val owner = accounts.first { it.id == entry.accountId }.owner as AccountOwner.User
                log.info("[WALLET] insufficient funds user={} reference={}", owner.userId, transaction.reference)
                throw InsufficientFundsException(owner.userId)
            }
        }
        ledgerRepository.insertEntries(transaction)

        log.info("[WALLET] transfer done reference={} kind={}", transaction.reference, transaction.kind)
        return TransferResult(transaction.id, userBalances(accounts))
    }

    private fun resolveAccount(ref: AccountRef): Account = when (ref) {
        is AccountRef.User -> accountRepository.getOrCreateUserAccount(ref.userId, clock.instant())
        is AccountRef.System -> accountRepository.getSystemAccount(ref.account)
    }

    private fun userBalances(accounts: List<Account>) =
        accounts.mapNotNull { account ->
            (account.owner as? AccountOwner.User)?.let { it.userId to accountRepository.balanceOf(account.id) }
        }.toMap()
}
