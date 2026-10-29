package ru.pogosian.kazik.common.business

import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

private val uuidRandom = SecureRandom()

fun uuidV7(now: Instant): UUID {
    val randomBits = ByteArray(10).also { uuidRandom.nextBytes(it) }
    val msb = (now.toEpochMilli() shl 16) or
        (0x7L shl 12) or
        ((randomBits[0].toLong() and 0x0F) shl 8) or
        (randomBits[1].toLong() and 0xFF)
    val lsb = (2 until 10).fold(0L) { acc, i -> (acc shl 8) or (randomBits[i].toLong() and 0xFF) }
    return UUID(msb, (lsb and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE)
}
