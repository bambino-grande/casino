package ru.pogosian.kazik.common.presentation.ExceptionHandler

import java.time.Instant

data class ErrorResponse(
    val code: String,
    val message: String,
    val status: Int,
    val timestamp: Instant,
    val traceId: String?,
)
