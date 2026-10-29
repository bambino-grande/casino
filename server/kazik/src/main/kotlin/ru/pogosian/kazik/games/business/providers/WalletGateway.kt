package ru.pogosian.kazik.games.business.providers

import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.UserId

interface WalletGateway {
    fun debitBet(userId: UserId, amount: NaeBalls, reference: String): NaeBalls

    fun creditWin(userId: UserId, amount: NaeBalls, reference: String): NaeBalls
}
