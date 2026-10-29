package ru.pogosian.kazik.wallet.business.services

import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.common.business.UserId
import java.util.UUID

interface TopUpCreditService {
    fun creditTopUp(eventId: UUID, paymentId: UUID, userId: UserId, amount: Rub)
}
