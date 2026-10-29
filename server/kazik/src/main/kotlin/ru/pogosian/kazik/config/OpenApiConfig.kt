package ru.pogosian.kazik.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val SECURITY_SCHEME = "keycloak"

@Configuration
class OpenApiConfig(
    @Value("\${kazik.swagger.oauth.authorization-url}") private val authorizationUrl: String,
    @Value("\${kazik.swagger.oauth.token-url}") private val tokenUrl: String,
) {
    @Bean
    fun kazikOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Kazik API")
                    .version("v1")
                    .description(
                        "Казино на наеБАЛЛАХ. 1) POST /auth/register, 2) Authorize, " +
                            "3) пополнить кошелёк картой Z-банка, 4) крутить слоты; за джекпот — картинка-приз, " +
                            "5) задонатить автору по СБП.",
                    ),
            )
            .components(
                Components().addSecuritySchemes(
                    SECURITY_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(
                            OAuthFlows().authorizationCode(
                                OAuthFlow()
                                    .authorizationUrl(authorizationUrl)
                                    .tokenUrl(tokenUrl)
                                    .scopes(Scopes().addString("openid", "OpenID Connect")),
                            ),
                        ),
                ),
            )
            .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME))
}
