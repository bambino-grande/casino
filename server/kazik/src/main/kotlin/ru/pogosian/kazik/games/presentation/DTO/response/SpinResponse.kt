package ru.pogosian.kazik.games.presentation.DTO.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class SpinResponse(
    val roundId: UUID,
    @field:Schema(description = "выпавшие символы трёх барабанов", example = "[\"CAPY\", \"CAPY\", \"CAPY\"]")
    val reels: List<String>,
    val bet: BigDecimal,
    @field:Schema(description = "выигрыш, включая джекпот")
    val win: BigDecimal,
    @field:Schema(description = "часть выигрыша из пула джекпота")
    val jackpotWin: BigDecimal,
    @field:Schema(description = "баланс после раунда; null, если это повтор запроса с тем же Idempotency-Key")
    val balanceAfter: BigDecimal?,
    @field:Schema(description = "что показать клиенту: NONE, COINS, CAPYBARA_HOT_TUB, CAPYBARA_RAIN (джекпот)")
    val animation: String,
    @field:Schema(description = "true — ответ на повтор запроса с тем же Idempotency-Key")
    val replayed: Boolean,
    @field:Schema(description = "картинка-приз за джекпот; null, если джекпота не было")
    val prize: PrizeInfoResponse?,
    val createdAt: Instant,
)

data class PrizeInfoResponse(
    val prizeId: UUID,
    @field:Schema(description = "адрес картинки в API", example = "/api/v1/rewards/prizes/0199.../image")
    val imageUrl: String,
)
