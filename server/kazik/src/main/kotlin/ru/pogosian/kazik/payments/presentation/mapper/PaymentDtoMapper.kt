package ru.pogosian.kazik.payments.presentation.mapper

import org.springframework.stereotype.Component
import ru.pogosian.kazik.payments.business.payments.DonationWallEntry
import ru.pogosian.kazik.payments.business.payments.Payment
import ru.pogosian.kazik.payments.business.services.PaymentDetails
import ru.pogosian.kazik.payments.presentation.DTO.response.DonationResponse
import ru.pogosian.kazik.payments.presentation.DTO.response.DonationWallEntryResponse
import ru.pogosian.kazik.payments.presentation.DTO.response.PaymentResponse
import ru.pogosian.kazik.payments.presentation.DTO.response.SbpTransferResponse

private const val ANONYMOUS_DONOR = "Аноним"

@Component
class PaymentDtoMapper {
    fun toDto(details: PaymentDetails): PaymentResponse =
        toDto(details.payment, details.donation?.let { DonationResponse(it.donorName, it.message, it.showOnWall) })

    fun toDto(payment: Payment): PaymentResponse = toDto(payment, null)

    fun toDto(entry: DonationWallEntry): DonationWallEntryResponse =
        DonationWallEntryResponse(entry.paymentId, entry.donorName ?: ANONYMOUS_DONOR, entry.message, entry.amount.toDecimal(), entry.donatedAt)

    private fun toDto(payment: Payment, donation: DonationResponse?): PaymentResponse =
        PaymentResponse(
            id = payment.id,
            purpose = payment.purpose.name,
            method = payment.method.name,
            amount = payment.amount.toDecimal(),
            status = payment.status.name,
            sbpTransfer = payment.sbpRequisites?.let { SbpTransferResponse(it.phone, it.bank, it.recipient, it.comment) },
            failureReason = payment.failureReason,
            expiresAt = payment.expiresAt,
            createdAt = payment.createdAt,
            succeededAt = payment.succeededAt,
            donation = donation,
        )
}
