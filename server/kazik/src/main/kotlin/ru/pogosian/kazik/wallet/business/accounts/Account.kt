package ru.pogosian.kazik.wallet.business.accounts

import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.wallet.api.SystemAccount
import java.util.UUID

data class Account(
    val id: UUID,
    val owner: AccountOwner,
    val balance: NaeBalls,
)

sealed interface AccountOwner {
    data class User(val userId: UserId) : AccountOwner

    data class System(val account: SystemAccount) : AccountOwner
}
