package ru.pogosian.kazik.payments.infrastructure.messaging

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionOperations
import ru.pogosian.kazik.common.infrastructure.messaging.IntegrationEvent
import ru.pogosian.kazik.common.infrastructure.messaging.IntegrationEventBus
import ru.pogosian.kazik.payments.infrastructure.config.OutboxProperties
import ru.pogosian.kazik.payments.infrastructure.repository.OutboxEvent
import ru.pogosian.kazik.payments.infrastructure.repository.OutboxRepository
import java.time.Clock

private const val MAX_BACKOFF_STEPS = 20L

@Component
class OutboxRelay(
    private val outboxRepository: OutboxRepository,
    private val eventBus: IntegrationEventBus,
    private val transactions: TransactionOperations,
    private val clock: Clock,
    private val properties: OutboxProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishPending(): Int {
        val batch = requireNotNull(
            transactions.execute {
                val now = clock.instant()
                outboxRepository.claimBatch(now, properties.batchSize, now.plus(properties.lease))
            },
        )
        batch.forEach(::publish)
        return batch.size
    }

    private fun publish(event: OutboxEvent) {
        try {
            eventBus.deliver(IntegrationEvent(event.id, event.eventType, event.aggregateId, event.payload, event.traceId))
            transactions.executeWithoutResult { outboxRepository.markPublished(event.id, clock.instant()) }
        } catch (exception: Exception) {
            val attempt = event.attempts + 1
            val retryAt = clock.instant().plus(properties.retryBackoff.multipliedBy(attempt.toLong().coerceAtMost(MAX_BACKOFF_STEPS)))
            log.warn("[OUTBOX] delivery of {} {} failed (attempt {}), retry at {}", event.eventType, event.id, attempt, retryAt, exception)
            transactions.executeWithoutResult {
                outboxRepository.markFailed(event.id, (exception.message ?: exception.javaClass.simpleName).take(500), retryAt)
            }
        }
    }
}
