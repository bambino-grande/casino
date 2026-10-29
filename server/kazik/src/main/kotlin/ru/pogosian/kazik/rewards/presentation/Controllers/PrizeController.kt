package ru.pogosian.kazik.rewards.presentation.Controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.security.CurrentUserProvider
import ru.pogosian.kazik.rewards.business.services.PrizeService
import ru.pogosian.kazik.rewards.presentation.DTO.response.PrizeResponse
import ru.pogosian.kazik.rewards.presentation.mapper.PrizeDtoMapper
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rewards")
@Tag(name = "rewards", description = "призы: картинки, которые выдаются за джекпот в слотах")
class PrizeController(
    private val prizeService: PrizeService,
    private val currentUserProvider: CurrentUserProvider,
    private val prizeDtoMapper: PrizeDtoMapper,
) {
    @GetMapping("/prizes")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "моя коллекция призов", description = "призы текущего пользователя, новые сверху. приз выдаётся за три капибары (джекпот)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "призы",
            content = [Content(array = ArraySchema(schema = Schema(implementation = PrizeResponse::class)))],
        ),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
    )
    fun getMyPrizes(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): List<PrizeResponse> =
        prizeService.getUserPrizes(currentUserProvider.current().id, PageQuery(page, size)).map(prizeDtoMapper::toDto)

    @GetMapping("/prizes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or @prizeSecurityService.isPrizeOwner(#id, authentication)")
    @Operation(summary = "получить приз", description = "USER видит только свои призы, ADMIN — любые")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "приз", content = [Content(schema = Schema(implementation = PrizeResponse::class))]),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
        ApiResponse(responseCode = "403", description = "чужой приз"),
        ApiResponse(responseCode = "404", description = "приз не найден"),
    )
    fun getPrize(@PathVariable id: UUID): PrizeResponse = prizeDtoMapper.toDto(prizeService.getPrize(id))

    @GetMapping("/prizes/{id}/image", produces = ["image/png", "image/jpeg", "image/webp", "image/gif", "image/svg+xml"])
    @PreAuthorize("hasAnyRole('ADMIN') or @prizeSecurityService.isPrizeOwner(#id, authentication)")
    @Operation(summary = "картинка приза", description = "сама картинка (файл из тома). отдаётся только владельцу приза и админу")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "картинка", content = [Content(mediaType = "image/*")]),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
        ApiResponse(responseCode = "403", description = "чужой приз"),
        ApiResponse(responseCode = "404", description = "приз не найден"),
        ApiResponse(responseCode = "503", description = "файл картинки пропал из тома"),
    )
    fun getImage(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val image = prizeService.getImage(id)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType))
            .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePrivate())
            .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
            .body(image.bytes)
    }
}