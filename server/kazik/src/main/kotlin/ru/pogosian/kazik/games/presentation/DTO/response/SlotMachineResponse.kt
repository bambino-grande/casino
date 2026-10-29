package ru.pogosian.kazik.games.presentation.DTO.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class SlotMachineResponse(
    val code: String,
    val title: String,
    val paytableVersion: Int,
    val minBet: BigDecimal,
    val maxBet: BigDecimal,
    @field:Schema(description = "вес символа на барабане: вероятность = вес / сумма весов")
    val reelWeights: Map<String, Int>,
    @field:Schema(description = "множитель ставки за три одинаковых символа; три CAPY — джекпот")
    val threeOfKind: Map<String, Long>,
    val twoCherries: Long,
    val twoCapybaras: Long,
    @field:Schema(description = "теоретический возврат игроку без учёта джекпота", example = "0.9298")
    val baseRtp: BigDecimal,
)
