package ru.pogosian.kazik.common.business

import java.math.BigDecimal
import java.math.RoundingMode

private const val NAE_BALLS_SCALE = 2

val ZERO_NAE_BALLS = NaeBalls(0)

@JvmInline
value class NaeBalls(val minor: Long) : Comparable<NaeBalls> {
    operator fun plus(other: NaeBalls): NaeBalls = NaeBalls(Math.addExact(minor, other.minor))

    operator fun minus(other: NaeBalls): NaeBalls = NaeBalls(Math.subtractExact(minor, other.minor))

    operator fun unaryMinus(): NaeBalls = NaeBalls(Math.negateExact(minor))

    operator fun times(multiplier: Long): NaeBalls = NaeBalls(Math.multiplyExact(minor, multiplier))

    fun share(basisPoints: Int): NaeBalls {
        require(basisPoints in 0..10_000) { "basis points must be in 0..10000" }
        return NaeBalls(Math.multiplyExact(minor, basisPoints.toLong()) / 10_000)
    }

    fun isPositive(): Boolean = minor > 0

    fun isNegative(): Boolean = minor < 0

    fun isZero(): Boolean = minor == 0L

    fun toDecimal(): BigDecimal = BigDecimal.valueOf(minor, NAE_BALLS_SCALE)

    override fun compareTo(other: NaeBalls): Int = minor.compareTo(other.minor)

    override fun toString(): String = toDecimal().toPlainString()
}

fun naeBallsOf(whole: Long): NaeBalls = NaeBalls(Math.multiplyExact(whole, 100L))

fun naeBallsOf(value: BigDecimal): NaeBalls {
    require(value.stripTrailingZeros().scale() <= NAE_BALLS_SCALE) { "amount must have at most $NAE_BALLS_SCALE decimal places" }
    return NaeBalls(value.setScale(NAE_BALLS_SCALE, RoundingMode.UNNECESSARY).unscaledValue().longValueExact())
}
