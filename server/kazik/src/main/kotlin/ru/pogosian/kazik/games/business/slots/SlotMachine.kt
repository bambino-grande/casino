package ru.pogosian.kazik.games.business.slots

import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.games.business.exceptions.InvalidBetException
import ru.pogosian.kazik.games.business.providers.RandomSource

const val SLOT_REELS = 3

enum class Symbol {
    CHERRY,
    LEMON,
    BELL,
    DIAMOND,
    SEVEN,
    CAPY,
}

enum class Animation {
    NONE,
    COINS,
    CAPYBARA_HOT_TUB,
    CAPYBARA_RAIN,
}

data class SpinOutcome(
    val multiplier: Long,
    val jackpot: Boolean,
)

data class SlotMachine(
    val code: String,
    val title: String,
    val paytableVersion: Int,
    val minBet: NaeBalls,
    val maxBet: NaeBalls,
    val reelWeights: Map<Symbol, Int>,
    val threeOfKind: Map<Symbol, Long>,
    val twoCherries: Long,
    val twoCapybaras: Long,
    val bigWinMultiplier: Long,
) {
    private val totalWeight = reelWeights.values.sum()
    private val cumulativeWeights = Symbol.entries.runningFold(0) { acc, symbol -> acc + reelWeights.getValue(symbol) }.drop(1)

    init {
        require(code.isNotBlank()) { "machine code must not be blank" }
        require(minBet.isPositive() && minBet <= maxBet) { "invalid bet limits of $code" }
        require(reelWeights.keys == Symbol.entries.toSet()) { "every symbol needs a weight" }
        require(reelWeights.values.all { it > 0 }) { "weights must be positive" }
        require(Symbol.CAPY !in threeOfKind) { "three capybaras pay the jackpot, not a multiplier" }
    }

    fun validateBet(bet: NaeBalls) {
        if (bet < minBet || bet > maxBet) throw InvalidBetException("Bet must be between $minBet and $maxBet naeballs on $code")
    }

    fun spin(random: RandomSource): List<Symbol> = List(SLOT_REELS) { pickSymbol(random.nextInt(totalWeight)) }

    fun evaluate(reels: List<Symbol>): SpinOutcome {
        require(reels.size == SLOT_REELS) { "slot has $SLOT_REELS reels" }
        val counts = reels.groupingBy { it }.eachCount()
        return when {
            counts[Symbol.CAPY] == SLOT_REELS -> SpinOutcome(0, true)
            counts.size == 1 -> SpinOutcome(threeOfKind.getValue(reels.first()), false)
            counts[Symbol.CHERRY] == 2 -> SpinOutcome(twoCherries, false)
            counts[Symbol.CAPY] == 2 -> SpinOutcome(twoCapybaras, false)
            else -> SpinOutcome(0, false)
        }
    }

    fun animationFor(outcome: SpinOutcome): Animation = when {
        outcome.jackpot -> Animation.CAPYBARA_RAIN
        outcome.multiplier >= bigWinMultiplier -> Animation.CAPYBARA_HOT_TUB
        outcome.multiplier > 0 -> Animation.COINS
        else -> Animation.NONE
    }

    fun baseRtp(): Double =
        Symbol.entries.flatMap { first -> Symbol.entries.flatMap { second -> Symbol.entries.map { third -> listOf(first, second, third) } } }
            .sumOf { reels -> evaluate(reels).multiplier * probabilityOf(reels) }

    private fun probabilityOf(reels: List<Symbol>): Double =
        reels.fold(1.0) { acc, symbol -> acc * reelWeights.getValue(symbol) / totalWeight }

    private fun pickSymbol(roll: Int): Symbol = Symbol.entries[cumulativeWeights.indexOfFirst { roll < it }]
}
