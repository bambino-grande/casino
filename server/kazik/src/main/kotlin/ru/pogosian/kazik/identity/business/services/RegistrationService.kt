package ru.pogosian.kazik.identity.business.services

import ru.pogosian.kazik.identity.business.accounts.NewAccount
import ru.pogosian.kazik.common.business.UserId

interface RegistrationService {
    fun register(account: NewAccount): UserId
}
