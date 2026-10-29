package ru.pogosian.kazik.wallet.api

import ru.pogosian.kazik.common.business.NaeBalls

data class Leg(
    val account: AccountRef,
    val amount: NaeBalls,
)
