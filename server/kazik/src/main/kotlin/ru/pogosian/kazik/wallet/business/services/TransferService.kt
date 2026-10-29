package ru.pogosian.kazik.wallet.business.services

import ru.pogosian.kazik.wallet.api.TransferCommand
import ru.pogosian.kazik.wallet.api.TransferResult

interface TransferService {
    fun transfer(command: TransferCommand): TransferResult
}
