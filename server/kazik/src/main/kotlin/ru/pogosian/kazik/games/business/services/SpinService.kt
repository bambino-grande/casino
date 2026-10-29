package ru.pogosian.kazik.games.business.services

import ru.pogosian.kazik.common.business.IdempotencyKey
import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.games.business.providers.GrantedPrize
import ru.pogosian.kazik.games.business.rounds.Round
import ru.pogosian.kazik.games.business.slots.Animation

data class SpinResult(
    val round: Round,
    val balanceAfter: NaeBalls?,
    val animation: Animation,
    val replayed: Boolean,
    val prize: GrantedPrize?,
)

interface SpinService {
    fun spin(userId: UserId, machineCode: String, bet: NaeBalls, idempotencyKey: IdempotencyKey): SpinResult
}
