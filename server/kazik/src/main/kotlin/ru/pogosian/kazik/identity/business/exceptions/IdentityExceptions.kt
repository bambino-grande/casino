package ru.pogosian.kazik.identity.business.exceptions

import ru.pogosian.kazik.common.business.exceptions.ConflictException
import ru.pogosian.kazik.common.business.exceptions.DomainValidationException
import ru.pogosian.kazik.common.business.exceptions.ExternalServiceUnavailableException

class InvalidRegistrationException(message: String) : DomainValidationException(message, "INVALID_REGISTRATION")

class WeakPasswordException(reason: String) : DomainValidationException("Password is too weak: $reason", "WEAK_PASSWORD")

class UsernameTakenException(username: String) :
    ConflictException("Username '$username' is already taken; if it is yours, just log in", "USERNAME_TAKEN")

class EmailTakenException : ConflictException("Account with this email already exists", "EMAIL_TAKEN")

class IdentityProviderUnavailableException(cause: Throwable? = null) :
    ExternalServiceUnavailableException("Identity provider is temporarily unavailable, retry later", "IDENTITY_PROVIDER_UNAVAILABLE") {
    init {
        cause?.let { initCause(it) }
    }
}
