package ru.pogosian.kazik.common.infrastructure.messaging

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import ru.pogosian.kazik.common.security.TRACE_ID

@Component
class InProcessIntegrationEventBus(
    handlers: List<IntegrationEventHandler>,
) : IntegrationEventBus {
    private val log = LoggerFactory.getLogger(javaClass)
    private val handlersByType = handlers.groupBy { it.eventType }

    override fun deliver(event: IntegrationEvent) {
        val subscribers = handlersByType[event.type].orEmpty()
        val previousTraceId = MDC.get(TRACE_ID)
        event.traceId?.let { MDC.put(TRACE_ID, it) }
        try {
            log.info("[EVENT BUS] deliver type={} eventId={} subscribers={}", event.type, event.eventId, subscribers.size)
            subscribers.forEach { it.handle(event) }
        } finally {
            if (previousTraceId != null) MDC.put(TRACE_ID, previousTraceId) else MDC.remove(TRACE_ID)
        }
    }
}
