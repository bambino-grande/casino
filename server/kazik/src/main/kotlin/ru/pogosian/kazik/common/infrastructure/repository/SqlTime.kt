package ru.pogosian.kazik.common.infrastructure.repository

import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

fun Instant.toSql(): Timestamp = Timestamp.from(this)

fun ResultSet.instant(column: String): Instant = getTimestamp(column).toInstant()

fun ResultSet.instantOrNull(column: String): Instant? = getTimestamp(column)?.toInstant()

fun ResultSet.longOrNull(column: String): Long? = getLong(column).takeUnless { wasNull() }
