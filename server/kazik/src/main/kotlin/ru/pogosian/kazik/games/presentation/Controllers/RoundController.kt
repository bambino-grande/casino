package ru.pogosian.kazik.games.presentation.Controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.pogosian.kazik.games.business.services.RoundService
import ru.pogosian.kazik.games.presentation.DTO.response.JackpotResponse
import ru.pogosian.kazik.games.presentation.DTO.response.RoundResponse
import ru.pogosian.kazik.games.presentation.mapper.GameMapper
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.security.CurrentUserProvider
import java.util.UUID

@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "rounds", description = "история раундов и джекпот")
class RoundController(
    private val roundService: RoundService,
    private val currentUserProvider: CurrentUserProvider,
    private val gameMapper: GameMapper,
) {
    @GetMapping("/rounds")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "мои раунды", description = "история раундов текущего пользователя, новые сверху")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "раунды",
            content = [Content(array = ArraySchema(schema = Schema(implementation = RoundResponse::class)))],
        ),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
    )
    fun getMyRounds(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): List<RoundResponse> =
        roundService.getUserRounds(currentUserProvider.current().id, PageQuery(page, size)).map(gameMapper::toDto)

    @GetMapping("/rounds/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or @roundSecurityService.isRoundOwner(#id, authentication)")
    @Operation(summary = "получить раунд", description = "USER видит только свои раунды, ADMIN — любые")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "раунд", content = [Content(schema = Schema(implementation = RoundResponse::class))]),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
        ApiResponse(responseCode = "403", description = "чужой раунд"),
        ApiResponse(responseCode = "404", description = "раунд не найден"),
    )
    fun getRound(@PathVariable id: UUID): RoundResponse = gameMapper.toDto(roundService.getRound(id))

    @GetMapping("/jackpot")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "джекпот", description = "текущий пул и последние выигрыши джекпота")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "джекпот", content = [Content(schema = Schema(implementation = JackpotResponse::class))]),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
    )
    fun getJackpot(): JackpotResponse = gameMapper.toDto(roundService.getJackpot())
}
