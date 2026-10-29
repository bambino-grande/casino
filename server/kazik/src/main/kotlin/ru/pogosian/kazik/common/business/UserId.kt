package ru.pogosian.kazik.common.business

import java.util.UUID

@JvmInline
value class UserId(val value: UUID) {
    override fun toString(): String = value.toString()
}

fun userIdOf(value: String): UserId = UserId(UUID.fromString(value))
