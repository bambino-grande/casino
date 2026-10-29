package ru.pogosian.kazik.wallet.infrastructure.repository

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.infrastructure.repository.instant
import ru.pogosian.kazik.common.infrastructure.repository.toSql
import ru.pogosian.kazik.wallet.api.TransferKind
import ru.pogosian.kazik.wallet.business.ledger.LedgerEntryView
import ru.pogosian.kazik.wallet.business.ledger.LedgerTransaction
import java.util.UUID

private val entryViewMapper = RowMapper { rs, _ ->
    LedgerEntryView(
        entryId = rs.getObject("id", UUID::class.java),
        amount = NaeBalls(rs.getLong("amount_minor")),
        reference = rs.getString("reference"),
        kind = TransferKind.valueOf(rs.getString("kind")),
        createdAt = rs.instant("created_at"),
    )
}

@Repository
class LedgerRepository(
    private val jdbcClient: JdbcClient,
) {
    fun insertTransaction(transaction: LedgerTransaction) {
        jdbcClient.sql("INSERT INTO ledger_transactions (id, reference, kind, created_at) VALUES (:id, :reference, :kind, :createdAt)")
            .param("id", transaction.id)
            .param("reference", transaction.reference)
            .param("kind", transaction.kind.name)
            .param("createdAt", transaction.createdAt.toSql())
            .update()
    }

    fun insertEntries(transaction: LedgerTransaction) {
        transaction.entries.forEach { entry ->
            jdbcClient.sql(
                """
                INSERT INTO ledger_entries (id, transaction_id, account_id, amount_minor, created_at)
                VALUES (:id, :transactionId, :accountId, :amount, :createdAt)
                """,
            )
                .param("id", entry.id)
                .param("transactionId", transaction.id)
                .param("accountId", entry.accountId)
                .param("amount", entry.amount.minor)
                .param("createdAt", transaction.createdAt.toSql())
                .update()
        }
    }

    fun findHistory(accountId: UUID, page: PageQuery): List<LedgerEntryView> =
        jdbcClient.sql(
            """
            SELECT e.id, e.amount_minor, t.reference, t.kind, e.created_at
            FROM ledger_entries e
            JOIN ledger_transactions t ON t.id = e.transaction_id
            WHERE e.account_id = :accountId
            ORDER BY e.id DESC
            LIMIT :limit OFFSET :offset
            """,
        )
            .param("accountId", accountId)
            .param("limit", page.size)
            .param("offset", page.offset)
            .query(entryViewMapper)
            .list()
}
