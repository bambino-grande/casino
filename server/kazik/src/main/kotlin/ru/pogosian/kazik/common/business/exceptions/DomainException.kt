package ru.pogosian.kazik.common.business.exceptions

abstract class DomainException(val code: String, message: String) : RuntimeException(message)

open class DomainValidationException(message: String, code: String) : DomainException(code, message)

open class NotFoundException(message: String, code: String) : DomainException(code, message)

open class ConflictException(message: String, code: String) : DomainException(code, message)

open class BusinessRuleException(message: String, code: String) : DomainException(code, message)

open class ExternalServiceUnavailableException(message: String, code: String) : DomainException(code, message)

class IdempotencyConflictException(key: String) :
    ConflictException("Idempotency-Key '$key' was already used with different parameters", "IDEMPOTENCY_KEY_REUSED")
