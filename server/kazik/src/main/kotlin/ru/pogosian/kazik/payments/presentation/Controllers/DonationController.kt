package ru.pogosian.kazik.payments.presentation.Controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
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
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.pogosian.kazik.common.business.IDEMPOTENCY_KEY_HEADER
import ru.pogosian.kazik.common.business.IdempotencyKey
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.rubOf
import ru.pogosian.kazik.common.security.CurrentUserProvider
import ru.pogosian.kazik.payments.business.services.NewDonation
import ru.pogosian.kazik.payments.business.services.PaymentService
import ru.pogosian.kazik.payments.presentation.DTO.request.CreateDonationRequest
import ru.pogosian.kazik.payments.presentation.DTO.response.DonationWallEntryResponse
import ru.pogosian.kazik.payments.presentation.DTO.response.PaymentResponse
import ru.pogosian.kazik.payments.presentation.mapper.PaymentDtoMapper

@RestController
@RequestMapping("/api/v1/donations")
@Tag(name = "donations", description = "донаты автору проекта по СБП — рублями, мимо кошелька наеБАЛЛОВ")
class DonationController(
    private val paymentService: PaymentService,
    private val currentUserProvider: CurrentUserProvider,
    private val paymentDtoMapper: PaymentDtoMapper,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER')")
    @Operation(
        summary = "задонатить автору",
        description = "донат засчитывается сразу и попадает на стену донатеров; в ответе реквизиты СБП и код-комментарий, " +
            "по которым нужно перевести деньги. перевод на личный номер сервер не проверяет",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "донат создан", content = [Content(schema = Schema(implementation = PaymentResponse::class))]),
        ApiResponse(responseCode = "400", description = "некорректные данные"),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
        ApiResponse(responseCode = "409", description = "Idempotency-Key уже использован с другими параметрами"),
    )
    fun createDonation(
        @Parameter(description = "уникальный ключ попытки, 8..64 символа", example = "donate-0001-a1b2")
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) idempotencyKey: String,
        @RequestBody request: CreateDonationRequest,
    ): PaymentResponse {
        val details = paymentService.createDonation(
            userId = currentUserProvider.current().id,
            amount = rubOf(request.amount),
            donation = NewDonation(request.donorName, request.message, request.showOnWall),
            idempotencyKey = IdempotencyKey(idempotencyKey),
        )
        return paymentDtoMapper.toDto(details)
    }

    @GetMapping
    @Operation(summary = "стена донатеров", description = "донаты, которые донатеры разрешили показывать; доступно без авторизации")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "донаты",
            content = [Content(array = ArraySchema(schema = Schema(implementation = DonationWallEntryResponse::class)))],
        ),
        ApiResponse(responseCode = "400", description = "page < 0 или size вне 1..100"),
    )
    fun getWall(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): List<DonationWallEntryResponse> = paymentService.getDonationWall(PageQuery(page, size)).map(paymentDtoMapper::toDto)
}
