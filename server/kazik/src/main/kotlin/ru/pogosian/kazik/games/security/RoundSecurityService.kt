package ru.pogosian.kazik.games.security

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import ru.pogosian.kazik.games.infrastructure.repository.RoundRepository
import java.util.UUID

@Component
class RoundSecurityService(
    private val roundRepository: RoundRepository,
) {
    fun isRoundOwner(id: UUID, authentication: Authentication): Boolean {
        val subject = (authentication as? JwtAuthenticationToken)?.token?.subject ?: return false
        val round = roundRepository.findById(id) ?: return true
        return round.userId.value.toString() == subject
    }
}
