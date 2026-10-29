package ru.pogosian.kazik.payments.business.services

import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionOperations
import ru.pogosian.kazik.common.business.IdempotencyKey
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.exceptions.IdempotencyConflictException
import ru.pogosian.kazik.common.business.uuidV7
import ru.pogosian.kazik.payments.api.PAYMENT_SUCCEEDED_EVENT
import ru.pogosian.kazik.payments.api.PaymentSucceededEvent
import ru.pogosian.kazik.payments.business.exceptions.PaymentDeclinedException
import ru.pogosian.kazik.payments.business.exceptions.PaymentNotFoundException
import ru.pogosian.kazik.payments.business.payments.Card
import ru.pogosian.kazik.payments.business.payments.Donation
import ru.pogosian.kazik.payments.business.payments.DonationWallEntry
import ru.pogosian.kazik.payments.business.payments.Payment
import ru.pogosian.kazik.payments.business.payments.PaymentPurpose
import ru.pogosian.kazik.payments.business.payments.PaymentRoutingPolicy
import ru.pogosian.kazik.payments.business.payments.PaymentStatus
import ru.pogosian.kazik.payments.business.providers.PaymentProviderRegistry
import ru.pogosian.kazik.payments.business.providers.ProviderPaymentRequest
import ru.pogosian.kazik.payments.business.providers.ProviderPaymentStatus
import ru.pogosian.kazik.payments.infrastructure.repository.DonationRepository
import ru.pogosian.kazik.payments.infrastructure.repository.OutboxRepository
import ru.pogosian.kazik.payments.infrastructure.repository.PaymentRepository
import java.time.Clock
import java.util.UUID

@Service
class PaymentServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val donationRepository: DonationRepository,
    private val outboxRepository: OutboxRepository,
    private val providers: PaymentProviderRegistry,
    private val routingPolicy: PaymentRoutingPolicy,
    private val transactions: TransactionOperations,
    private val clock: Clock,
) : PaymentService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun createTopUp(userId: UserId, amount: Rub, card: Card, idempotencyKey: IdempotencyKey): PaymentDetails {
        val payment = submit(findOrCreate(userId, PaymentPurpose.WALLET_TOP_UP, amount, idempotencyKey, null), card)
        if (payment.status == PaymentStatus.FAILED) throw PaymentDeclinedException(payment.failureReason)
        return PaymentDetails(payment, null)
    }

    override fun createDonation(userId: UserId, amount: Rub, donation: NewDonation, idempotencyKey: IdempotencyKey): PaymentDetails {
        val payment = submit(findOrCreate(userId, PaymentPurpose.AUTHOR_DONATION, amount, idempotencyKey, donation), null)
        return PaymentDetails(payment, donationRepository.findByPaymentId(payment.id))
    }

    @Transactional(readOnly = true)
    override fun getPayment(id: UUID): PaymentDetails {
        val payment = paymentRepository.findById(id) ?: throw PaymentNotFoundException(id)
        return PaymentDetails(payment, donationRepository.findByPaymentId(id))
    }

    @Transactional(readOnly = true)
    override fun getUserPayments(userId: UserId, page: PageQuery): List<Payment> = paymentRepository.findByUser(userId, page)

    @Transactional(readOnly = true)
    override fun getDonationWall(page: PageQuery): List<DonationWallEntry> = donationRepository.findWall(page)

    private fun findOrCreate(userId: UserId, purpose: PaymentPurpose, amount: Rub, key: IdempotencyKey, donation: NewDonation?): Payment {
        routingPolicy.validateAmount(purpose, amount)
        paymentRepository.findByUserAndKey(userId, key)?.let { return checkSameRequest(it, purpose, amount) }

        val now = clock.instant()
        val rule = routingPolicy.ruleFor(purpose)
        val payment = Payment(
            id = uuidV7(now),
            operationId = uuidV7(now),
            userId = userId,
            idempotencyKey = key,
            method = rule.method,
            purpose = purpose,
            amount = amount,
            status = PaymentStatus.CREATED,
            providerPaymentId = null,
            sbpRequisites = null,
            failureReason = null,
            expiresAt = now.plus(rule.timeToPay),
            succeededAt = null,
            createdAt = now,
            version = 0,
        )
        return try {
            transactions.executeWithoutResult {
                paymentRepository.insert(payment)
                donation?.let {
                    donationRepository.insert(Donation(payment.id, it.donorName?.trim()?.ifBlank { null }, it.message?.trim()?.ifBlank { null }, it.showOnWall), now)
                }
            }
            log.info("[PAYMENTS] payment {} created purpose={} method={} amount={}", payment.id, purpose, payment.method, amount)
            payment
        } catch (exception: DuplicateKeyException) {
            checkSameRequest(paymentRepository.findByUserAndKey(userId, key) ?: throw exception, purpose, amount)
        }
    }

    private fun submit(payment: Payment, card: Card?): Payment {
        if (!payment.status.canBeSubmitted()) return payment
        val result = providers.get(payment.method).create(ProviderPaymentRequest(payment.operationId, payment.amount, describe(payment), card))

        return requireNotNull(
            transactions.execute {
                val current = paymentRepository.findById(payment.id) ?: throw PaymentNotFoundException(payment.id)
                if (!current.status.canBeSubmitted()) return@execute current
                val now = clock.instant()
                val updated = when (result.status) {
                    ProviderPaymentStatus.SUCCEEDED -> current.succeed(result.providerPaymentId, result.sbpRequisites, now)
                    ProviderPaymentStatus.DECLINED -> current.decline(result.providerPaymentId, result.declineCode ?: "DECLINED")
                }
                val saved = paymentRepository.update(updated)
                if (saved.status == PaymentStatus.SUCCEEDED) {
                    outboxRepository.add(
                        PAYMENT_SUCCEEDED_EVENT,
                        saved.id,
                        PaymentSucceededEvent(saved.id, saved.userId.value, saved.purpose.name, saved.amount.kopecks, now),
                        now,
                    )
                }
                log.info("[PAYMENTS] payment {} {} -> {} via {}", saved.id, current.status, saved.status, saved.method)
                saved
            },
        )
    }

    private fun checkSameRequest(existing: Payment, purpose: PaymentPurpose, amount: Rub): Payment {
        if (!existing.isSameRequest(purpose, amount)) throw IdempotencyConflictException(existing.idempotencyKey.value)
        return existing
    }

    private fun describe(payment: Payment): String = when (payment.purpose) {
        PaymentPurpose.WALLET_TOP_UP -> "Kazik: пополнение на ${payment.amount} наеБАЛЛОВ"
        PaymentPurpose.AUTHOR_DONATION -> "Kazik: донат автору"
    }
}
