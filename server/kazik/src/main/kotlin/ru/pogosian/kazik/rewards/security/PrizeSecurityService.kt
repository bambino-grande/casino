package ru.pogosian.kazik.rewards.security

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import ru.pogosian.kazik.rewards.infrastructure.repository.PrizeRepository
import java.util.UUID

@Component
class PrizeSecurityService(
    private val prizeRepository: PrizeRepository,
) {
    fun isPrizeOwner(id: UUID, authentication: Authentication): Boolean {
        val subject = (authentication as? JwtAuthenticationToken)?.token?.subject ?: return false
        val prize = prizeRepository.findById(id) ?: return true
        return prize.userId.value.toString() == subject
    }
}
