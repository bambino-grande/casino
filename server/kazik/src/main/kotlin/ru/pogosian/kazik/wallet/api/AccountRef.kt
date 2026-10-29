package ru.pogosian.kazik.wallet.api

import ru.pogosian.kazik.common.business.UserId

sealed interface AccountRef {
    data class User(val userId: UserId) : AccountRef

    data class System(val account: SystemAccount) : AccountRef
}
