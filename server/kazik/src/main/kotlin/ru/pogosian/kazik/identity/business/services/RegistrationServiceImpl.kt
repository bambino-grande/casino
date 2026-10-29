package ru.pogosian.kazik.identity.business.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.pogosian.kazik.identity.business.accounts.NewAccount
import ru.pogosian.kazik.identity.business.providers.IdentityProvider
import ru.pogosian.kazik.common.business.UserId

@Service
class RegistrationServiceImpl(
    private val identityProvider: IdentityProvider,
) : RegistrationService {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun register(account: NewAccount): UserId {
        val userId = identityProvider.register(account)
        log.info("[IDENTITY] registered user {}", userId)
        return userId
    }
}
