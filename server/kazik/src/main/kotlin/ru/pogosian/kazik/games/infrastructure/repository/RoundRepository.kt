package ru.pogosian.kazik.games.infrastructure.repository

import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.pogosian.kazik.common.business.IdempotencyKey
import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.infrastructure.repository.instant
import ru.pogosian.kazik.common.infrastructure.repository.toSql
import ru.pogosian.kazik.games.business.exceptions.DuplicateRoundException
import ru.pogosian.kazik.games.business.rounds.Round
import ru.pogosian.kazik.games.business.slots.Symbol
import java.util.UUID

private const val ROUND_COLUMNS = "id, user_id, idempotency_key, machine_code, paytable_version, bet_minor, reels, win_minor, jackpot_minor, created_at"

private val roundMapper = RowMapper { rs, _ ->
    Round(
        id = rs.getObject("id", UUID::class.java),
        userId = UserId(rs.getObject("user_id", UUID::class.java)),
        idempotencyKey = IdempotencyKey(rs.getString("idempotency_key")),
        machineCode = rs.getString("machine_code"),
        paytableVersion = rs.getInt("paytable_version"),
        bet = NaeBalls(rs.getLong("bet_minor")),
        reels = rs.getString("reels").split(",").map(Symbol::valueOf),
        win = NaeBalls(rs.getLong("win_minor")),
        jackpotWin = NaeBalls(rs.getLong("jackpot_minor")),
        createdAt = rs.instant("created_at"),
    )
}

@Repository
class RoundRepository(
    private val jdbcClient: JdbcClient,
) {
    fun insert(round: Round): Round {
        try {
            jdbcClient.sql(
                """
                INSERT INTO rounds (id, user_id, idempotency_key, machine_code, paytable_version, bet_minor, reels, win_minor, jackpot_minor, created_at)
                VALUES (:id, :userId, :key, :machineCode, :paytableVersion, :bet, :reels, :win, :jackpot, :createdAt)
                """,
            )
                .param("id", round.id)
                .param("userId", round.userId.value)
                .param("key", round.idempotencyKey.value)
                .param("machineCode", round.machineCode)
                .param("paytableVersion", round.paytableVersion)
                .param("bet", round.bet.minor)
                .param("reels", round.reels.joinToString(",") { it.name })
                .param("win", round.win.minor)
                .param("jackpot", round.jackpotWin.minor)
                .param("createdAt", round.createdAt.toSql())
                .update()
        } catch (exception: DuplicateKeyException) {
            throw DuplicateRoundException()
        }
        return round
    }

    fun findById(id: UUID): Round? =
        jdbcClient.sql("SELECT $ROUND_COLUMNS FROM rounds WHERE id = :id")
            .param("id", id)
            .query(roundMapper)
            .list()
            .firstOrNull()

    fun findByUserAndKey(userId: UserId, key: IdempotencyKey): Round? =
        jdbcClient.sql("SELECT $ROUND_COLUMNS FROM rounds WHERE user_id = :userId AND idempotency_key = :key")
            .param("userId", userId.value)
            .param("key", key.value)
            .query(roundMapper)
            .list()
            .firstOrNull()

    fun findByUser(userId: UserId, page: PageQuery): List<Round> =
        jdbcClient.sql("SELECT $ROUND_COLUMNS FROM rounds WHERE user_id = :userId ORDER BY id DESC LIMIT :limit OFFSET :offset")
            .param("userId", userId.value)
            .param("limit", page.size)
            .param("offset", page.offset)
            .query(roundMapper)
            .list()

    fun findRecentJackpots(limit: Int): List<Round> =
        jdbcClient.sql("SELECT $ROUND_COLUMNS FROM rounds WHERE jackpot_minor > 0 ORDER BY id DESC LIMIT :limit")
            .param("limit", limit)
            .query(roundMapper)
            .list()
}
