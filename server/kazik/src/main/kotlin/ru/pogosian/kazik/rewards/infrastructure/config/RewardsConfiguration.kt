package ru.pogosian.kazik.rewards.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "kazik.rewards")
data class RewardsProperties(
    val imagesDir: String,
)

@Configuration
@EnableConfigurationProperties(RewardsProperties::class)
class RewardsConfiguration
