package ru.pogosian.kazik.common.business

import ru.pogosian.kazik.common.business.exceptions.DomainValidationException

const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

private val IDEMPOTENCY_KEY_FORMAT = Regex("[A-Za-z0-9_-]{8,64}")

@JvmInline
value class IdempotencyKey(val value: String) {
    init {
        if (!IDEMPOTENCY_KEY_FORMAT.matches(value)) {
            throw DomainValidationException("Idempotency-Key must be 8..64 characters of letters, digits, '-' or '_'", "INVALID_IDEMPOTENCY_KEY")
        }
    }

    override fun toString(): String = value
}
