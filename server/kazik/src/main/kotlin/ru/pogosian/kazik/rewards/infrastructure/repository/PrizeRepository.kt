package ru.pogosian.kazik.rewards.infrastructure.repository

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.infrastructure.repository.instant
import ru.pogosian.kazik.common.infrastructure.repository.toSql
import ru.pogosian.kazik.rewards.business.prizes.Prize
import java.util.UUID

private const val PRIZE_COLUMNS = "id, user_id, round_id, image_file, created_at"

private val prizeMapper = RowMapper { rs, _ ->
    Prize(
        id = rs.getObject("id", UUID::class.java),
        userId = UserId(rs.getObject("user_id", UUID::class.java)),
        roundId = rs.getObject("round_id", UUID::class.java),
        imageFile = rs.getString("image_file"),
        grantedAt = rs.instant("created_at"),
    )
}

@Repository
class PrizeRepository(
    private val jdbcClient: JdbcClient,
) {
    fun insert(prize: Prize) {
        jdbcClient.sql("INSERT INTO prizes (id, user_id, round_id, image_file, created_at) VALUES (:id, :userId, :roundId, :imageFile, :createdAt)")
            .param("id", prize.id)
            .param("userId", prize.userId.value)
            .param("roundId", prize.roundId)
            .param("imageFile", prize.imageFile)
            .param("createdAt", prize.grantedAt.toSql())
            .update()
    }

    fun findById(id: UUID): Prize? =
        jdbcClient.sql("SELECT $PRIZE_COLUMNS FROM prizes WHERE id = :id").param("id", id).query(prizeMapper).list().firstOrNull()

    fun findByRound(roundId: UUID): Prize? =
        jdbcClient.sql("SELECT $PRIZE_COLUMNS FROM prizes WHERE round_id = :roundId").param("roundId", roundId).query(prizeMapper).list().firstOrNull()

    fun findByUser(userId: UserId, page: PageQuery): List<Prize> =
        jdbcClient.sql("SELECT $PRIZE_COLUMNS FROM prizes WHERE user_id = :userId ORDER BY id DESC LIMIT :limit OFFSET :offset")
            .param("userId", userId.value)
            .param("limit", page.size)
            .param("offset", page.offset)
            .query(prizeMapper)
            .list()
}
