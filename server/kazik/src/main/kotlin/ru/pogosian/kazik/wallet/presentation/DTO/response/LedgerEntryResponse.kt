package ru.pogosian.kazik.wallet.presentation.DTO.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class LedgerEntryResponse(
    val entryId: UUID,
    @field:Schema(description = "изменение баланса: плюс — зачисление, минус — списание", example = "-10.00")
    val amount: BigDecimal,
    @field:Schema(description = "операция-источник", example = "round:0199...:bet")
    val reference: String,
    @field:Schema(description = "тип операции: TOP_UP, BET, WIN")
    val kind: String,
    val createdAt: Instant,
)
