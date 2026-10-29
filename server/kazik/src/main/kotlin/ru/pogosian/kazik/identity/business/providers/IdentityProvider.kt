package ru.pogosian.kazik.identity.business.providers

import ru.pogosian.kazik.identity.business.accounts.NewAccount
import ru.pogosian.kazik.common.business.UserId

interface IdentityProvider {
    fun register(account: NewAccount): UserId
}
