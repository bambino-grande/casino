package ru.pogosian.kazik.common.presentation.ExceptionHandler

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import ru.pogosian.kazik.common.business.exceptions.BusinessRuleException
import ru.pogosian.kazik.common.business.exceptions.ConflictException
import ru.pogosian.kazik.common.business.exceptions.DomainException
import ru.pogosian.kazik.common.business.exceptions.DomainValidationException
import ru.pogosian.kazik.common.business.exceptions.ExternalServiceUnavailableException
import ru.pogosian.kazik.common.business.exceptions.NotFoundException
import ru.pogosian.kazik.common.security.TRACE_ID
import java.time.Instant

@RestControllerAdvice
class Handler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(DomainValidationException::class)
    fun handleDomainValidationException(exception: DomainValidationException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, exception)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(exception: NotFoundException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.NOT_FOUND, exception)

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(exception: ConflictException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.CONFLICT, exception)

    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRuleException(exception: BusinessRuleException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.UNPROCESSABLE_CONTENT, exception)

    @ExceptionHandler(ExternalServiceUnavailableException::class)
    fun handleExternalServiceUnavailableException(exception: ExternalServiceUnavailableException): ResponseEntity<ErrorResponse> {
        log.warn("[ERROR HANDLER] external service unavailable: {}", exception.message)
        return respond(HttpStatus.SERVICE_UNAVAILABLE, exception)
    }

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicateKeyException(exception: DuplicateKeyException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.CONFLICT, "DUPLICATE", "The same operation is already being processed, retry later")

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailureException(exception: OptimisticLockingFailureException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Resource was modified concurrently, reload it and retry")

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingRequestHeaderException(exception: MissingRequestHeaderException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, "MISSING_HEADER", "Required header '${exception.headerName}' is missing")

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameterException(exception: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "Required parameter '${exception.parameterName}' is missing")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(exception: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Parameter '${exception.name}' has invalid value")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(exception: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, "MALFORMED_BODY", "Request body is missing, malformed or lacks a required field")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(exception: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.message ?: "Invalid request")

    @ExceptionHandler(AccessDeniedException::class, AuthorizationDeniedException::class)
    fun handleAccessDeniedException(exception: RuntimeException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.FORBIDDEN, "FORBIDDEN", "Not enough permissions for this operation")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupportedException(exception: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", exception.message ?: "Method not allowed")

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(exception: NoResourceFoundException): ResponseEntity<ErrorResponse> =
        respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found")

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ResponseEntity<ErrorResponse> {
        log.error("[ERROR HANDLER] unexpected error", exception)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error")
    }

    private fun respond(status: HttpStatus, exception: DomainException): ResponseEntity<ErrorResponse> =
        respond(status, exception.code, exception.message ?: status.reasonPhrase)

    private fun respond(status: HttpStatus, code: String, message: String): ResponseEntity<ErrorResponse> {
        log.info("[ERROR HANDLER] status={} code={} message={}", status.value(), code, message)
        return ResponseEntity.status(status)
            .body(ErrorResponse(code, message, status.value(), Instant.now(), MDC.get(TRACE_ID)))
    }
}
