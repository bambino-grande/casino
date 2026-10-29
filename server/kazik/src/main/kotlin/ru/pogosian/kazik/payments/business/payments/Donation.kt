package ru.pogosian.kazik.payments.business.payments

import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.payments.business.exceptions.InvalidDonationException
import java.time.Instant
import java.util.UUID

private const val MAX_DONOR_NAME = 64

private const val MAX_DONATION_MESSAGE = 500

data class Donation(
    val paymentId: UUID,
    val donorName: String?,
    val message: String?,
    val showOnWall: Boolean,
) {
    init {
        if (donorName != null && donorName.length > MAX_DONOR_NAME) throw InvalidDonationException("donor name is longer than $MAX_DONOR_NAME characters")
        if (message != null && message.length > MAX_DONATION_MESSAGE) throw InvalidDonationException("message is longer than $MAX_DONATION_MESSAGE characters")
    }
}

data class DonationWallEntry(
    val paymentId: UUID,
    val donorName: String?,
    val message: String?,
    val amount: Rub,
    val donatedAt: Instant,
)
