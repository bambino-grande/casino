package ru.pogosian.kazik.identity.presentation.Controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.pogosian.kazik.identity.business.accounts.NewAccount
import ru.pogosian.kazik.identity.business.services.RegistrationService
import ru.pogosian.kazik.identity.presentation.DTO.request.RegisterRequest
import ru.pogosian.kazik.identity.presentation.DTO.response.MeResponse
import ru.pogosian.kazik.identity.presentation.DTO.response.RegisterResponse
import ru.pogosian.kazik.common.security.CurrentUserProvider

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth", description = "регистрация; вход — кнопкой Authorize в Swagger (Keycloak, PKCE)")
class AuthController(
    private val registrationService: RegistrationService,
    private val currentUserProvider: CurrentUserProvider,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "зарегистрироваться", description = "создаёт пользователя в Keycloak с ролью USER. затем войдите через Authorize")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "пользователь создан", content = [Content(schema = Schema(implementation = RegisterResponse::class))]),
        ApiResponse(responseCode = "400", description = "некорректные данные или слабый пароль"),
        ApiResponse(responseCode = "409", description = "username или email уже заняты"),
        ApiResponse(responseCode = "503", description = "Keycloak недоступен"),
    )
    fun register(@RequestBody request: RegisterRequest): RegisterResponse {
        val account = NewAccount(request.username, request.email, request.password)
        return RegisterResponse(registrationService.register(account).value, account.username)
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "кто я", description = "id и роли из токена")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "текущий пользователь", content = [Content(schema = Schema(implementation = MeResponse::class))]),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
    )
    fun me(): MeResponse {
        val user = currentUserProvider.current()
        return MeResponse(user.id.value, user.username, user.roles.map { it.name }.sorted())
    }
}
