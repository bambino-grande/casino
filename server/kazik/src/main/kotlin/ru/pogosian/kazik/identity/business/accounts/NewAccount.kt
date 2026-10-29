package ru.pogosian.kazik.identity.business.accounts

import ru.pogosian.kazik.identity.business.exceptions.InvalidRegistrationException

private val USERNAME_FORMAT = Regex("[a-z0-9_.-]{3,32}")

private val EMAIL_FORMAT = Regex("[^@\\s]{1,64}@[^@\\s]{1,255}\\.[^@\\s]{2,}")

class NewAccount(
    username: String,
    email: String,
    val password: String,
) {
    val username: String = username.trim().lowercase()
    val email: String = email.trim().lowercase()

    init {
        if (!USERNAME_FORMAT.matches(this.username)) throw InvalidRegistrationException("username must be 3..32 characters of a-z, 0-9, '_', '.', '-'")
        if (!EMAIL_FORMAT.matches(this.email)) throw InvalidRegistrationException("email is invalid")
        if (password.length !in 8..128) throw InvalidRegistrationException("password must be 8..128 characters")
    }

    override fun toString(): String = "NewAccount(username=$username)"
}
