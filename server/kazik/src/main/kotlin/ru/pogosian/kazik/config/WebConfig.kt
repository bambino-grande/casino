package ru.pogosian.kazik.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import ru.pogosian.kazik.common.security.TraceIdInterceptor

@Configuration
class WebConfig(
    private val traceIdInterceptor: TraceIdInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addWebRequestInterceptor(traceIdInterceptor)
    }
}
