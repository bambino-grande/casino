package ru.pogosian.kazik.games.infrastructure.repository

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.games.business.jackpot.JackpotPool
import ru.pogosian.kazik.games.business.jackpot.MAIN_JACKPOT
import java.util.UUID

private val poolMapper = RowMapper { rs, _ ->
    JackpotPool(
        id = rs.getObject("id", UUID::class.java),
        code = rs.getString("code"),
        amount = NaeBalls(rs.getLong("amount_minor")),
        seed = NaeBalls(rs.getLong("seed_minor")),
    )
}

@Repository
class JackpotPoolRepository(
    private val jdbcClient: JdbcClient,
) {
    fun lockMain(): JackpotPool = findMain("FOR UPDATE")

    fun findMain(): JackpotPool = findMain("")

    fun save(pool: JackpotPool) {
        jdbcClient.sql("UPDATE jackpot_pools SET amount_minor = :amount WHERE id = :id")
            .param("id", pool.id)
            .param("amount", pool.amount.minor)
            .update()
    }

    private fun findMain(lock: String): JackpotPool =
        jdbcClient.sql("SELECT id, code, amount_minor, seed_minor FROM jackpot_pools WHERE code = :code $lock")
            .param("code", MAIN_JACKPOT)
            .query(poolMapper)
            .list()
            .firstOrNull()
            ?: error("jackpot pool $MAIN_JACKPOT is missing, check migrations")
}
