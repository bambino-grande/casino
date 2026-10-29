package ru.pogosian.kazik.games.presentation.Controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.pogosian.kazik.common.business.IDEMPOTENCY_KEY_HEADER
import ru.pogosian.kazik.common.business.IdempotencyKey
import ru.pogosian.kazik.common.business.naeBallsOf
import ru.pogosian.kazik.common.security.CurrentUserProvider
import ru.pogosian.kazik.games.business.services.RoundService
import ru.pogosian.kazik.games.business.services.SpinService
import ru.pogosian.kazik.games.presentation.DTO.request.SpinRequest
import ru.pogosian.kazik.games.presentation.DTO.response.SlotMachineResponse
import ru.pogosian.kazik.games.presentation.DTO.response.SpinResponse
import ru.pogosian.kazik.games.presentation.mapper.GameMapper

@RestController
@RequestMapping("/api/v1/games/slots")
@Tag(name = "slots", description = "слоты: список автоматов и спин")
class SlotController(
    private val spinService: SpinService,
    private val roundService: RoundService,
    private val currentUserProvider: CurrentUserProvider,
    private val gameMapper: GameMapper,
) {
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "список автоматов", description = "возвращает автоматы с лимитами ставок, таблицей выплат и теоретическим RTP")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "автоматы",
            content = [Content(array = ArraySchema(schema = Schema(implementation = SlotMachineResponse::class)))],
        ),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
    )
    fun getMachines(): List<SlotMachineResponse> = roundService.getMachines().map(gameMapper::toDto)

    @PostMapping("/{machineCode}/spins")
    @PreAuthorize("hasAnyRole('USER')")
    @Operation(
        summary = "крутануть барабаны",
        description = "списывает ставку, крутит барабаны и зачисляет выигрыш. повтор с тем же Idempotency-Key " +
            "возвращает тот же раунд и не списывает ставку второй раз. три CAPY — джекпот и анимация CAPYBARA_RAIN",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "результат раунда", content = [Content(schema = Schema(implementation = SpinResponse::class))]),
        ApiResponse(responseCode = "400", description = "некорректная ставка или Idempotency-Key"),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
        ApiResponse(responseCode = "403", description = "недостаточно прав"),
        ApiResponse(responseCode = "404", description = "автомат не найден"),
        ApiResponse(responseCode = "409", description = "ключ уже использован с другой ставкой или раунд ещё выполняется"),
        ApiResponse(responseCode = "422", description = "не хватает наеБАЛЛОВ"),
        ApiResponse(responseCode = "503", description = "кошелёк временно недоступен, раунд будет сверен автоматически"),
    )
    fun spin(
        @PathVariable machineCode: String,
        @Parameter(description = "уникальный ключ попытки, 8..64 символа", example = "spin-0001-a1b2")
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) idempotencyKey: String,
        @RequestBody request: SpinRequest,
    ): SpinResponse {
        val user = currentUserProvider.current()
        val result = spinService.spin(user.id, machineCode, naeBallsOf(request.bet), IdempotencyKey(idempotencyKey))
        return gameMapper.toDto(result)
    }
}
