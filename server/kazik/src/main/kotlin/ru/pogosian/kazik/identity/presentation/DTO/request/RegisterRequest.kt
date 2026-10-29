package ru.pogosian.kazik.identity.presentation.DTO.request

import io.swagger.v3.oas.annotations.media.Schema

data class RegisterRequest(
    @field:Schema(description = "3..32 символа: a-z, 0-9, '_', '.', '-'", example = "capy_fan")
    val username: String,
    @field:Schema(example = "capy_fan@example.com")
    val email: String,
    @field:Schema(description = "8..128 символов", example = "S3cret-capy!")
    val password: String,
) {
    override fun toString(): String = "RegisterRequest(username=$username)"
}
