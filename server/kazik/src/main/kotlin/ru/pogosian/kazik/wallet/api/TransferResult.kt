package ru.pogosian.kazik.wallet.api

import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.UserId
import java.util.UUID

data class TransferResult(
    val transactionId: UUID,
    val balances: Map<UserId, NaeBalls>,
)
