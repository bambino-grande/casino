package ru.pogosian.kazik.wallet.presentation.mapper

import org.springframework.stereotype.Component
import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.wallet.business.ledger.LedgerEntryView
import ru.pogosian.kazik.wallet.presentation.DTO.response.LedgerEntryResponse
import ru.pogosian.kazik.wallet.presentation.DTO.response.WalletResponse

private const val CURRENCY_NAME = "наеБАЛЛЫ"

@Component
class WalletMapper {
    fun toDto(userId: UserId, balance: NaeBalls): WalletResponse = WalletResponse(userId.value, balance.toDecimal(), CURRENCY_NAME)

    fun toDto(entry: LedgerEntryView): LedgerEntryResponse =
        LedgerEntryResponse(entry.entryId, entry.amount.toDecimal(), entry.reference, entry.kind.name, entry.createdAt)
}
