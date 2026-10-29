package ru.pogosian.kazik.games.infrastructure.random

import org.springframework.stereotype.Component
import ru.pogosian.kazik.games.business.providers.RandomSource
import java.security.SecureRandom

@Component
class SecureRandomSource : RandomSource {
    private val random = SecureRandom()

    override fun nextInt(bound: Int): Int = random.nextInt(bound)
}
