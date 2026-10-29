package ru.pogosian.kazik.wallet.presentation.Controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.security.CurrentUserProvider
import ru.pogosian.kazik.wallet.business.services.WalletService
import ru.pogosian.kazik.wallet.presentation.DTO.response.LedgerEntryResponse
import ru.pogosian.kazik.wallet.presentation.DTO.response.WalletResponse
import ru.pogosian.kazik.wallet.presentation.mapper.WalletMapper

@RestController
@RequestMapping("/api/v1/wallet")
@Tag(name = "wallet", description = "кошелёк наеБАЛЛОВ: баланс и история операций")
class WalletController(
    private val walletService: WalletService,
    private val currentUserProvider: CurrentUserProvider,
    private val walletMapper: WalletMapper,
) {
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "получить баланс", description = "возвращает баланс текущего пользователя. счёт создаётся при первой операции, до неё баланс 0")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "баланс", content = [Content(schema = Schema(implementation = WalletResponse::class))]),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
    )
    fun getWallet(): WalletResponse {
        val user = currentUserProvider.current()
        return walletMapper.toDto(user.id, walletService.getBalance(user.id))
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "история операций", description = "проводки по счёту текущего пользователя, новые сверху")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "список проводок",
            content = [Content(array = ArraySchema(schema = Schema(implementation = LedgerEntryResponse::class)))],
        ),
        ApiResponse(responseCode = "400", description = "некорректные параметры страницы"),
        ApiResponse(responseCode = "401", description = "пользователь не авторизован"),
    )
    fun getTransactions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): List<LedgerEntryResponse> {
        val user = currentUserProvider.current()
        return walletService.getHistory(user.id, PageQuery(page, size)).map(walletMapper::toDto)
    }
}
