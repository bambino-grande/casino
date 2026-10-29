package ru.pogosian.kazik.wallet.business.ledger

import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.wallet.api.TransferKind
import java.time.Instant
import java.util.UUID

data class LedgerEntryView(
    val entryId: UUID,
    val amount: NaeBalls,
    val reference: String,
    val kind: TransferKind,
    val createdAt: Instant,
)
