package ru.pogosian.kazik.common.security

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import ru.pogosian.kazik.common.business.userIdOf

@Component
class SpringCurrentUserProvider : CurrentUserProvider {
    override fun current(): CurrentUser {
        val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
            ?: throw AuthenticationCredentialsNotFoundException("Request is not authenticated")
        val jwt = authentication.token
        return CurrentUser(
            id = userIdOf(requireNotNull(jwt.subject) { "JWT has no subject" }),
            username = jwt.getClaimAsString("preferred_username"),
            roles = rolesOf(jwt),
        )
    }
}
