package ru.pogosian.kazik.wallet.api

data class TransferCommand(
    val reference: String,
    val kind: TransferKind,
    val legs: List<Leg>,
)
