package ru.pogosian.kazik.rewards.business.prizes

import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.rewards.business.exceptions.InvalidPrizeException
import java.time.Instant
import java.util.UUID

private const val MAX_FILE_NAME = 200

private val PRIZE_FILE_NAME = Regex("[^/\\\\\\u0000]{1,$MAX_FILE_NAME}")

data class Prize(
    val id: UUID,
    val userId: UserId,
    val roundId: UUID,
    val imageFile: String,
    val grantedAt: Instant,
) {
    init {
        if (!PRIZE_FILE_NAME.matches(imageFile)) throw InvalidPrizeException("image file must be a plain file name of 1..$MAX_FILE_NAME characters")
    }
}
