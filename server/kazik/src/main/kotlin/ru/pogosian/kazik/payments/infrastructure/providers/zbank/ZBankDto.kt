package ru.pogosian.kazik.payments.infrastructure.providers.zbank

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class ZBankPaymentRequest(
    val number: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val amountKopecks: Long,
) {
    override fun toString(): String = "ZBankPaymentRequest(number=****${number.takeLast(4)}, amountKopecks=$amountKopecks)"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ZBankOperation(
    val id: String,
    val kind: String,
    val status: String,
    val amountKopecks: Long,
    val destinationAccountId: String?,
    val errorCode: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ZBankError(
    val code: String,
    val message: String?,
)
