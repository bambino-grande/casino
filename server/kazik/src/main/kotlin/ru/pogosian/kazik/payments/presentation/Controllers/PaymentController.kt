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
import org.springframework.web.bind.annotation.PathVariable
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
import ru.pogosian.kazik.payments.business.payments.Card
import ru.pogosian.kazik.payments.business.services.PaymentService
import ru.pogosian.kazik.payments.presentation.DTO.request.CreateTopUpRequest
import ru.pogosian.kazik.payments.presentation.DTO.response.PaymentResponse
import ru.pogosian.kazik.payments.presentation.mapper.PaymentDtoMapper
import java.util.UUID

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "payments", description = "пополнение наеБАЛЛОВ картой Z-банка и статусы платежей")
class PaymentController(
    private val paymentService: PaymentService,
    private val currentUserProvider: CurrentUserProvider,
    private val paymentDtoMapper: PaymentDtoMapper,
) {
    @PostMapping("/top-ups")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER')")
    @Operation(
        summary = "пополнить кошелёк картой Z-банка",
        description = "списывает рубли с учебной карты Z-банка сразу, в этом же запросе. при успехе статус SUCCEEDED, " +
            "через секунду наеБАЛЛЫ по курсу 1 к 1 будут на кошельке. карта и CVV нигде не сохраняются. " +
            "при 503 результат неизвестен — повторите запрос с тем же Idempotency-Key и той же картой, второго списания не будет",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "оплата прошла", content = [Content(schema = Schema(implementation = PaymentResponse::class))]),
        ApiResponse(responseCode = "400", description = "сумма вне лимитов, неверный формат карты или Idempotency-Key"),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
        ApiResponse(responseCode = "409", description = "Idempotency-Key уже использован с другой суммой или картой"),
        ApiResponse(responseCode = "422", description = "банк отказал: CARD_INSUFFICIENT_FUNDS, INVALID_CARD_DETAILS, CARD_UNAVAILABLE"),
        ApiResponse(responseCode = "503", description = "результат неизвестен: повторите с тем же ключом и той же картой"),
    )
    fun createTopUp(
        @Parameter(description = "уникальный ключ попытки, 8..64 символа", example = "topup-0001-a1b2")
        @RequestHeader(IDEMPOTENCY_KEY_HEADER) idempotencyKey: String,
        @RequestBody request: CreateTopUpRequest,
    ): PaymentResponse {
        val card = Card(request.card.number, request.card.expiryMonth, request.card.expiryYear, request.card.cvv)
        val details = paymentService.createTopUp(currentUserProvider.current().id, rubOf(request.amount), card, IdempotencyKey(idempotencyKey))
        return paymentDtoMapper.toDto(details)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN') or @paymentSecurityService.isPaymentOwner(#id, authentication)")
    @Operation(summary = "получить платёж", description = "статус платежа и реквизиты. USER видит только свои платежи")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "платёж", content = [Content(schema = Schema(implementation = PaymentResponse::class))]),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
        ApiResponse(responseCode = "403", description = "чужой платёж"),
        ApiResponse(responseCode = "404", description = "платёж не найден"),
    )
    fun getPayment(@PathVariable id: UUID): PaymentResponse = paymentDtoMapper.toDto(paymentService.getPayment(id))

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "мои платежи", description = "платежи текущего пользователя, новые сверху")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "платежи",
            content = [Content(array = ArraySchema(schema = Schema(implementation = PaymentResponse::class)))],
        ),
        ApiResponse(responseCode = "400", description = "page < 0 или size вне 1..100"),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
    )
    fun getMyPayments(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): List<PaymentResponse> =
        paymentService.getUserPayments(currentUserProvider.current().id, PageQuery(page, size)).map(paymentDtoMapper::toDto)
}
