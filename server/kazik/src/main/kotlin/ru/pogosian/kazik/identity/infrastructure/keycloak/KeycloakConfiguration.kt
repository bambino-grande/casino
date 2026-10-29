package ru.pogosian.kazik.identity.infrastructure.keycloak

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties::class)
class KeycloakConfiguration {
    @Bean
    fun keycloakRestClient(properties: KeycloakAdminProperties, builder: RestClient.Builder): RestClient =
        builder
            .baseUrl(properties.baseUrl)
            .requestFactory(
                JdkClientHttpRequestFactory(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(properties.connectTimeout).build())
                    .apply { setReadTimeout(properties.readTimeout) },
            )
            .build()
}
