package ru.pogosian.kazik.wallet.business.ledger

import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.ZERO_NAE_BALLS
import ru.pogosian.kazik.wallet.api.TransferKind
import ru.pogosian.kazik.wallet.business.exceptions.UnbalancedTransferException
import java.time.Instant
import java.util.UUID

private const val MAX_REFERENCE_LENGTH = 200

data class LedgerEntry(
    val id: UUID,
    val accountId: UUID,
    val amount: NaeBalls,
)

data class LedgerTransaction(
    val id: UUID,
    val reference: String,
    val kind: TransferKind,
    val entries: List<LedgerEntry>,
    val createdAt: Instant,
) {
    init {
        if (reference.isBlank() || reference.length > MAX_REFERENCE_LENGTH) {
            throw UnbalancedTransferException("reference must be 1..$MAX_REFERENCE_LENGTH characters")
        }
        if (entries.size < 2) throw UnbalancedTransferException("transfer needs at least two accounts")
        if (entries.map { it.accountId }.toSet().size != entries.size) throw UnbalancedTransferException("every account may appear only once")
        if (entries.any { it.amount.isZero() }) throw UnbalancedTransferException("entry amount must not be zero")
        val total = entries.fold(ZERO_NAE_BALLS) { acc, entry -> acc + entry.amount }
        if (!total.isZero()) throw UnbalancedTransferException("entries must sum to zero, got $total")
    }
}
