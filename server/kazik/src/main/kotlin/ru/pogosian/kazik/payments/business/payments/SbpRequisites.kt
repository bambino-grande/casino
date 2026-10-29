package ru.pogosian.kazik.payments.business.payments

data class SbpRequisites(
    val phone: String,
    val bank: String,
    val recipient: String,
    val comment: String,
)
