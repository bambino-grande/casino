package ru.pogosian.kazik.wallet.business.services

import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.wallet.business.ledger.LedgerEntryView

interface WalletService {
    fun getBalance(userId: UserId): NaeBalls

    fun getHistory(userId: UserId, page: PageQuery): List<LedgerEntryView>
}
