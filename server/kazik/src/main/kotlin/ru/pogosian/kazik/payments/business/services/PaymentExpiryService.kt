package ru.pogosian.kazik.payments.business.services

interface PaymentExpiryService {
    fun expireOverdue(): Int
}
