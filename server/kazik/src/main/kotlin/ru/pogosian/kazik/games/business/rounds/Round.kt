package ru.pogosian.kazik.games.business.rounds

import ru.pogosian.kazik.common.business.IdempotencyKey
import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.games.business.slots.Symbol
import java.time.Instant
import java.util.UUID

data class Round(
    val id: UUID,
    val userId: UserId,
    val idempotencyKey: IdempotencyKey,
    val machineCode: String,
    val paytableVersion: Int,
    val bet: NaeBalls,
    val reels: List<Symbol>,
    val win: NaeBalls,
    val jackpotWin: NaeBalls,
    val createdAt: Instant,
) {
    fun isSameRequest(machineCode: String, bet: NaeBalls): Boolean = this.machineCode == machineCode && this.bet == bet
}

fun betReference(roundId: UUID): String = "round:$roundId:bet"

fun winReference(roundId: UUID): String = "round:$roundId:win"
