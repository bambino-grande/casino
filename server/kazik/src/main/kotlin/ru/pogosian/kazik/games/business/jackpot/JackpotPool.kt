package ru.pogosian.kazik.games.business.jackpot

import ru.pogosian.kazik.common.business.NaeBalls
import java.util.UUID

const val MAIN_JACKPOT = "MAIN"

data class JackpotPool(
    val id: UUID,
    val code: String,
    val amount: NaeBalls,
    val seed: NaeBalls,
) {
    init {
        require(!amount.isNegative()) { "jackpot pool must not be negative" }
    }

    fun contribute(contribution: NaeBalls): JackpotPool {
        require(!contribution.isNegative()) { "contribution must not be negative" }
        return copy(amount = amount + contribution)
    }

    fun reset(): JackpotPool = copy(amount = seed)
}

data class JackpotPolicy(
    val contributionBasisPoints: Int,
) {
    init {
        require(contributionBasisPoints in 0..10_000) { "contribution must be in 0..10000 bps" }
    }

    fun contributionFrom(bet: NaeBalls): NaeBalls = bet.share(contributionBasisPoints)
}
