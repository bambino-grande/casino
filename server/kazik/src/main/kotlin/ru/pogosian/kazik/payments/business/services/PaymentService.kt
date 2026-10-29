package ru.pogosian.kazik.payments.business.services

import ru.pogosian.kazik.common.business.IdempotencyKey
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.payments.business.payments.Card
import ru.pogosian.kazik.payments.business.payments.Donation
import ru.pogosian.kazik.payments.business.payments.DonationWallEntry
import ru.pogosian.kazik.payments.business.payments.Payment
import java.util.UUID

data class NewDonation(
    val donorName: String?,
    val message: String?,
    val showOnWall: Boolean,
)

data class PaymentDetails(
    val payment: Payment,
    val donation: Donation?,
)

interface PaymentService {
    fun createTopUp(userId: UserId, amount: Rub, card: Card, idempotencyKey: IdempotencyKey): PaymentDetails

    fun createDonation(userId: UserId, amount: Rub, donation: NewDonation, idempotencyKey: IdempotencyKey): PaymentDetails

    fun getPayment(id: UUID): PaymentDetails

    fun getUserPayments(userId: UserId, page: PageQuery): List<Payment>

    fun getDonationWall(page: PageQuery): List<DonationWallEntry>
}
