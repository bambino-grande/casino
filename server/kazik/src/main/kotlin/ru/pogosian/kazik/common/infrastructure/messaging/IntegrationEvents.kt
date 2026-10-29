package ru.pogosian.kazik.common.infrastructure.messaging

import java.util.UUID

data class IntegrationEvent(
    val eventId: UUID,
    val type: String,
    val aggregateId: UUID,
    val payload: String,
    val traceId: String?,
)

interface IntegrationEventHandler {
    val eventType: String

    fun handle(event: IntegrationEvent)
}

interface IntegrationEventBus {
    fun deliver(event: IntegrationEvent)
}
