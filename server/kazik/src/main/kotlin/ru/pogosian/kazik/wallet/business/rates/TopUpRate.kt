package ru.pogosian.kazik.wallet.business.rates

import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.Rub

data class TopUpRate(val naeBallsPerRub: Long) {
    init {
        require(naeBallsPerRub > 0) { "rate must be positive" }
    }

    fun toNaeBalls(amount: Rub): NaeBalls = NaeBalls(Math.multiplyExact(amount.kopecks, naeBallsPerRub))
}
