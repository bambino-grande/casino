package ru.pogosian.kazik.wallet.infrastructure.repository

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.pogosian.kazik.common.business.uuidV7
import ru.pogosian.kazik.common.infrastructure.repository.toSql
import java.time.Instant
import java.util.UUID

@Repository
class InboxRepository(
    private val jdbcClient: JdbcClient,
) {
    fun tryMarkProcessed(consumer: String, eventId: UUID, now: Instant): Boolean =
        jdbcClient.sql(
            """
            INSERT INTO inbox_messages (id, consumer, event_id, created_at)
            VALUES (:id, :consumer, :eventId, :createdAt)
            ON CONFLICT (consumer, event_id) DO NOTHING
            """,
        )
            .param("id", uuidV7(now))
            .param("consumer", consumer)
            .param("eventId", eventId)
            .param("createdAt", now.toSql())
            .update() == 1
}
