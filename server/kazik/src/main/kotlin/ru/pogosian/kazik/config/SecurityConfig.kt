package ru.pogosian.kazik.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import ru.pogosian.kazik.common.security.KeycloakJwtConverter

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val keycloakJwtConverter: KeycloakJwtConverter,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                it.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/donations").permitAll()
                it.requestMatchers("/error").permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth -> oauth.jwt { it.jwtAuthenticationConverter(keycloakJwtConverter) } }
        return http.build()
    }
}
