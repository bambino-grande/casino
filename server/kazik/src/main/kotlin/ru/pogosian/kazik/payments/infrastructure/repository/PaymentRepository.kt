package ru.pogosian.kazik.payments.infrastructure.repository

import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.pogosian.kazik.common.business.IdempotencyKey
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.infrastructure.repository.instant
import ru.pogosian.kazik.common.infrastructure.repository.instantOrNull
import ru.pogosian.kazik.common.infrastructure.repository.toSql
import ru.pogosian.kazik.payments.business.payments.Payment
import ru.pogosian.kazik.payments.business.payments.PaymentMethod
import ru.pogosian.kazik.payments.business.payments.PaymentPurpose
import ru.pogosian.kazik.payments.business.payments.PaymentStatus
import ru.pogosian.kazik.payments.business.payments.SbpRequisites
import java.time.Instant
import java.util.UUID

private const val PAYMENT_COLUMNS =
    "id, operation_id, user_id, idempotency_key, method, purpose, amount_kopecks, status, provider_payment_id, " +
        "transfer_phone, transfer_bank, transfer_recipient, transfer_comment, failure_reason, expires_at, succeeded_at, version, created_at"

private val paymentMapper = RowMapper { rs, _ ->
    Payment(
        id = rs.getObject("id", UUID::class.java),
        operationId = rs.getObject("operation_id", UUID::class.java),
        userId = UserId(rs.getObject("user_id", UUID::class.java)),
        idempotencyKey = IdempotencyKey(rs.getString("idempotency_key")),
        method = PaymentMethod.valueOf(rs.getString("method")),
        purpose = PaymentPurpose.valueOf(rs.getString("purpose")),
        amount = Rub(rs.getLong("amount_kopecks")),
        status = PaymentStatus.valueOf(rs.getString("status")),
        providerPaymentId = rs.getString("provider_payment_id"),
        sbpRequisites = rs.getString("transfer_phone")?.let { phone ->
            SbpRequisites(phone, rs.getString("transfer_bank"), rs.getString("transfer_recipient"), rs.getString("transfer_comment"))
        },
        failureReason = rs.getString("failure_reason"),
        expiresAt = rs.instant("expires_at"),
        succeededAt = rs.instantOrNull("succeeded_at"),
        createdAt = rs.instant("created_at"),
        version = rs.getLong("version"),
    )
}

@Repository
class PaymentRepository(
    private val jdbcClient: JdbcClient,
) {
    fun insert(payment: Payment) {
        jdbcClient.sql(
            """
            INSERT INTO payments (id, operation_id, user_id, idempotency_key, method, purpose, amount_kopecks, status, expires_at, version, created_at)
            VALUES (:id, :operationId, :userId, :key, :method, :purpose, :amount, :status, :expiresAt, :version, :createdAt)
            """,
        )
            .param("id", payment.id)
            .param("operationId", payment.operationId)
            .param("userId", payment.userId.value)
            .param("key", payment.idempotencyKey.value)
            .param("method", payment.method.name)
            .param("purpose", payment.purpose.name)
            .param("amount", payment.amount.kopecks)
            .param("status", payment.status.name)
            .param("expiresAt", payment.expiresAt.toSql())
            .param("version", payment.version)
            .param("createdAt", payment.createdAt.toSql())
            .update()
    }

    fun update(payment: Payment): Payment {
        val updated = jdbcClient.sql(
            """
            UPDATE payments
            SET status = :status, provider_payment_id = :providerPaymentId, transfer_phone = :phone, transfer_bank = :bank,
                transfer_recipient = :recipient, transfer_comment = :comment, failure_reason = :failureReason,
                succeeded_at = :succeededAt, version = version + 1
            WHERE id = :id AND version = :version
            """,
        )
            .param("id", payment.id)
            .param("version", payment.version)
            .param("status", payment.status.name)
            .param("providerPaymentId", payment.providerPaymentId)
            .param("phone", payment.sbpRequisites?.phone)
            .param("bank", payment.sbpRequisites?.bank)
            .param("recipient", payment.sbpRequisites?.recipient)
            .param("comment", payment.sbpRequisites?.comment)
            .param("failureReason", payment.failureReason)
            .param("succeededAt", payment.succeededAt?.toSql())
            .update()
        if (updated != 1) throw OptimisticLockingFailureException("payment ${payment.id} was changed concurrently")
        return payment.copy(version = payment.version + 1)
    }

    fun findById(id: UUID): Payment? =
        jdbcClient.sql("SELECT $PAYMENT_COLUMNS FROM payments WHERE id = :id")
            .param("id", id)
            .query(paymentMapper)
            .list()
            .firstOrNull()

    fun findByUserAndKey(userId: UserId, key: IdempotencyKey): Payment? =
        jdbcClient.sql("SELECT $PAYMENT_COLUMNS FROM payments WHERE user_id = :userId AND idempotency_key = :key")
            .param("userId", userId.value)
            .param("key", key.value)
            .query(paymentMapper)
            .list()
            .firstOrNull()

    fun findByUser(userId: UserId, page: PageQuery): List<Payment> =
        jdbcClient.sql("SELECT $PAYMENT_COLUMNS FROM payments WHERE user_id = :userId ORDER BY id DESC LIMIT :limit OFFSET :offset")
            .param("userId", userId.value)
            .param("limit", page.size)
            .param("offset", page.offset)
            .query(paymentMapper)
            .list()

    fun expireOverdue(now: Instant): Int =
        jdbcClient.sql("UPDATE payments SET status = 'EXPIRED', version = version + 1 WHERE status = 'CREATED' AND expires_at <= :now")
            .param("now", now.toSql())
            .update()
}
