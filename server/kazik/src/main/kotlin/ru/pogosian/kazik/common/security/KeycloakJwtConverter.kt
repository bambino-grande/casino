package ru.pogosian.kazik.common.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class KeycloakJwtConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val authorities = rolesOf(jwt).map { SimpleGrantedAuthority("ROLE_${it.name}") }
        return JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username") ?: requireNotNull(jwt.subject))
    }
}

fun rolesOf(jwt: Jwt): Set<Role> {
    val realmAccess = jwt.getClaimAsMap("realm_access") ?: return emptySet()
    val roles = realmAccess["roles"] as? Collection<*> ?: return emptySet()
    return roles.mapNotNull { role -> Role.entries.firstOrNull { it.name == role.toString().uppercase() } }.toSet()
}
