package ru.pogosian.kazik.payments.infrastructure.repository

import org.slf4j.MDC
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.pogosian.kazik.common.business.uuidV7
import ru.pogosian.kazik.common.infrastructure.repository.toSql
import ru.pogosian.kazik.common.security.TRACE_ID
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

data class OutboxEvent(
    val id: UUID,
    val aggregateId: UUID,
    val eventType: String,
    val payload: String,
    val traceId: String?,
    val attempts: Int,
)

private val outboxEventMapper = RowMapper { rs, _ ->
    OutboxEvent(
        id = rs.getObject("id", UUID::class.java),
        aggregateId = rs.getObject("aggregate_id", UUID::class.java),
        eventType = rs.getString("event_type"),
        payload = rs.getString("payload"),
        traceId = rs.getString("trace_id"),
        attempts = rs.getInt("attempts"),
    )
}

@Repository
class OutboxRepository(
    private val jdbcClient: JdbcClient,
    private val objectMapper: ObjectMapper,
) {
    fun add(eventType: String, aggregateId: UUID, payload: Any, now: Instant) {
        jdbcClient.sql(
            """
            INSERT INTO outbox_events (id, aggregate_id, event_type, payload, trace_id, attempts, created_at)
            VALUES (:id, :aggregateId, :eventType, :payload, :traceId, 0, :createdAt)
            """,
        )
            .param("id", uuidV7(now))
            .param("aggregateId", aggregateId)
            .param("eventType", eventType)
            .param("payload", objectMapper.writeValueAsString(payload))
            .param("traceId", MDC.get(TRACE_ID))
            .param("createdAt", now.toSql())
            .update()
    }

    fun claimBatch(now: Instant, limit: Int, leaseUntil: Instant): List<OutboxEvent> {
        val events = jdbcClient.sql(
            """
            SELECT id, aggregate_id, event_type, payload, trace_id, attempts FROM outbox_events
            WHERE published_at IS NULL AND (locked_until IS NULL OR locked_until < :now)
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """,
        )
            .param("now", now.toSql())
            .param("limit", limit)
            .query(outboxEventMapper)
            .list()
        events.forEach { event ->
            jdbcClient.sql("UPDATE outbox_events SET locked_until = :until WHERE id = :id")
                .param("id", event.id)
                .param("until", leaseUntil.toSql())
                .update()
        }
        return events
    }

    fun markPublished(id: UUID, now: Instant) {
        jdbcClient.sql("UPDATE outbox_events SET published_at = :now, locked_until = NULL WHERE id = :id")
            .param("id", id)
            .param("now", now.toSql())
            .update()
    }

    fun markFailed(id: UUID, error: String, retryAt: Instant) {
        jdbcClient.sql("UPDATE outbox_events SET attempts = attempts + 1, last_error = :error, locked_until = :retryAt WHERE id = :id")
            .param("id", id)
            .param("error", error)
            .param("retryAt", retryAt.toSql())
            .update()
    }
}
