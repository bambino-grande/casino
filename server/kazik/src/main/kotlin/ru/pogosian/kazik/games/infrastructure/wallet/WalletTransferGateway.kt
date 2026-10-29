package ru.pogosian.kazik.games.infrastructure.wallet

import org.springframework.stereotype.Component
import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.games.business.exceptions.InsufficientNaeBallsException
import ru.pogosian.kazik.games.business.providers.WalletGateway
import ru.pogosian.kazik.wallet.api.AccountRef
import ru.pogosian.kazik.wallet.api.Leg
import ru.pogosian.kazik.wallet.api.SystemAccount
import ru.pogosian.kazik.wallet.api.TransferCommand
import ru.pogosian.kazik.wallet.api.TransferKind
import ru.pogosian.kazik.wallet.business.exceptions.InsufficientFundsException
import ru.pogosian.kazik.wallet.business.services.TransferService

@Component
class WalletTransferGateway(
    private val transferService: TransferService,
) : WalletGateway {
    override fun debitBet(userId: UserId, amount: NaeBalls, reference: String): NaeBalls =
        try {
            transfer(userId, reference, TransferKind.BET, -amount)
        } catch (exception: InsufficientFundsException) {
            throw InsufficientNaeBallsException()
        }

    override fun creditWin(userId: UserId, amount: NaeBalls, reference: String): NaeBalls =
        transfer(userId, reference, TransferKind.WIN, amount)

    private fun transfer(userId: UserId, reference: String, kind: TransferKind, userDelta: NaeBalls): NaeBalls {
        val command = TransferCommand(
            reference = reference,
            kind = kind,
            legs = listOf(
                Leg(AccountRef.User(userId), userDelta),
                Leg(AccountRef.System(SystemAccount.HOUSE), -userDelta),
            ),
        )
        return transferService.transfer(command).balances.getValue(userId)
    }
}
