package ru.pogosian.kazik.common.security

import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.ui.ModelMap
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.WebRequestInterceptor
import java.util.UUID

const val TRACE_ID = "traceId"

@Component
class TraceIdInterceptor : WebRequestInterceptor {
    override fun preHandle(request: WebRequest) {
        val traceId = request.getHeader(TRACE_ID)?.takeIf { it.isNotBlank() && it.length <= 64 } ?: UUID.randomUUID().toString()
        MDC.put(TRACE_ID, traceId)
        (request as? ServletWebRequest)?.response?.setHeader(TRACE_ID, traceId)
    }

    override fun postHandle(request: WebRequest, model: ModelMap?) = Unit

    override fun afterCompletion(request: WebRequest, ex: Exception?) {
        MDC.remove(TRACE_ID)
    }
}
