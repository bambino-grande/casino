package ru.pogosian.kazik.wallet.infrastructure.repository

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.uuidV7
import ru.pogosian.kazik.common.infrastructure.repository.toSql
import ru.pogosian.kazik.wallet.api.SystemAccount
import ru.pogosian.kazik.wallet.business.accounts.Account
import ru.pogosian.kazik.wallet.business.accounts.AccountOwner
import java.time.Instant
import java.util.UUID

private const val ACCOUNT_COLUMNS = "id, owner_type, owner_id, system_code, balance_minor"

private val accountMapper = RowMapper { rs, _ ->
    Account(
        id = rs.getObject("id", UUID::class.java),
        owner = when (rs.getString("owner_type")) {
            "USER" -> AccountOwner.User(UserId(rs.getObject("owner_id", UUID::class.java)))
            else -> AccountOwner.System(SystemAccount.valueOf(rs.getString("system_code")))
        },
        balance = NaeBalls(rs.getLong("balance_minor")),
    )
}

@Repository
class AccountRepository(
    private val jdbcClient: JdbcClient,
) {
    fun findUserAccount(userId: UserId): Account? =
        jdbcClient.sql("SELECT $ACCOUNT_COLUMNS FROM accounts WHERE owner_type = 'USER' AND owner_id = :ownerId")
            .param("ownerId", userId.value)
            .query(accountMapper)
            .list()
            .firstOrNull()

    fun getOrCreateUserAccount(userId: UserId, now: Instant): Account {
        findUserAccount(userId)?.let { return it }
        jdbcClient.sql(
            """
            INSERT INTO accounts (id, owner_type, owner_id, system_code, balance_minor, created_at)
            VALUES (:id, 'USER', :ownerId, NULL, 0, :createdAt)
            ON CONFLICT (owner_id) WHERE owner_type = 'USER' DO NOTHING
            """,
        )
            .param("id", uuidV7(now))
            .param("ownerId", userId.value)
            .param("createdAt", now.toSql())
            .update()
        return findUserAccount(userId) ?: error("account of user $userId was not created")
    }

    fun getSystemAccount(account: SystemAccount): Account =
        jdbcClient.sql("SELECT $ACCOUNT_COLUMNS FROM accounts WHERE owner_type = 'SYSTEM' AND system_code = :code")
            .param("code", account.name)
            .query(accountMapper)
            .list()
            .firstOrNull()
            ?: error("system account $account is missing, check migrations")

    fun applyDelta(accountId: UUID, delta: NaeBalls): Boolean =
        jdbcClient.sql(
            """
            UPDATE accounts SET balance_minor = balance_minor + :delta
            WHERE id = :id AND (owner_type = 'SYSTEM' OR balance_minor + :delta >= 0)
            """,
        )
            .param("id", accountId)
            .param("delta", delta.minor)
            .update() == 1

    fun balanceOf(accountId: UUID): NaeBalls =
        NaeBalls(
            jdbcClient.sql("SELECT balance_minor FROM accounts WHERE id = :id")
                .param("id", accountId)
                .query(Long::class.java)
                .single(),
        )
}
