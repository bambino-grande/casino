package ru.pogosian.kazik.common.security

import ru.pogosian.kazik.common.business.UserId

enum class Role {
    USER,
    ADMIN,
}

data class CurrentUser(
    val id: UserId,
    val username: String?,
    val roles: Set<Role>,
)

interface CurrentUserProvider {
    fun current(): CurrentUser
}
