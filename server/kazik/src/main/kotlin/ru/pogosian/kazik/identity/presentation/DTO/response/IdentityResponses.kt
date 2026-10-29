package ru.pogosian.kazik.identity.presentation.DTO.response

import java.util.UUID

data class RegisterResponse(
    val userId: UUID,
    val username: String,
)

data class MeResponse(
    val userId: UUID,
    val username: String?,
    val roles: List<String>,
)
