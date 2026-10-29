package ru.pogosian.kazik

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MigrationIntegrationTest(support: IntegrationTestSupport) : BaseIntegrationTest(support) {
    @Test
    fun allMigrationsAreApplied() {
        val applied = jdbcTemplate.queryForList("SELECT filename FROM databasechangelog ORDER BY orderexecuted", String::class.java)
        assertEquals(listOf("001-wallet.sql", "002-payments.sql", "003-games.sql", "004-rewards.sql", "005-data.sql"), applied.map { it!!.substringAfterLast('/') })
    }

    @Test
    fun everyForeignKeyIsCoveredByIndex() {
        val unindexed = jdbcTemplate.queryForList(
            """
            SELECT c.conrelid::regclass || '(' || a.attname || ')'
            FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
            WHERE c.contype = 'f'
              AND c.connamespace = 'public'::regnamespace
              AND NOT EXISTS (
                  SELECT 1 FROM pg_index i
                  WHERE i.indrelid = c.conrelid AND i.indkey[0] = c.conkey[1]
              )
            """,
            String::class.java,
        )
        assertTrue(unindexed.isEmpty(), "foreign keys without index: $unindexed")
    }
}
