package ru.pogosian.kazik.common.business

import java.math.BigDecimal
import java.math.RoundingMode

private const val RUB_SCALE = 2

@JvmInline
value class Rub(val kopecks: Long) : Comparable<Rub> {
    init {
        require(kopecks >= 0) { "rubles amount must not be negative" }
    }

    fun toDecimal(): BigDecimal = BigDecimal.valueOf(kopecks, RUB_SCALE)

    override fun compareTo(other: Rub): Int = kopecks.compareTo(other.kopecks)

    override fun toString(): String = toDecimal().toPlainString()
}

fun rubOf(value: BigDecimal): Rub {
    require(value.stripTrailingZeros().scale() <= RUB_SCALE) { "amount must have at most $RUB_SCALE decimal places" }
    return Rub(value.setScale(RUB_SCALE, RoundingMode.UNNECESSARY).unscaledValue().longValueExact())
}
