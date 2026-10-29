package ru.pogosian.kazik.payments.infrastructure.repository

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.pogosian.kazik.common.business.PageQuery
import ru.pogosian.kazik.common.business.Rub
import ru.pogosian.kazik.common.infrastructure.repository.instant
import ru.pogosian.kazik.common.infrastructure.repository.toSql
import ru.pogosian.kazik.payments.business.payments.Donation
import ru.pogosian.kazik.payments.business.payments.DonationWallEntry
import java.time.Instant
import java.util.UUID

private val donationMapper = RowMapper { rs, _ ->
    Donation(rs.getObject("id", UUID::class.java), rs.getString("donor_name"), rs.getString("message"), rs.getBoolean("show_on_wall"))
}

private val wallEntryMapper = RowMapper { rs, _ ->
    DonationWallEntry(
        paymentId = rs.getObject("id", UUID::class.java),
        donorName = rs.getString("donor_name"),
        message = rs.getString("message"),
        amount = Rub(rs.getLong("amount_kopecks")),
        donatedAt = rs.instant("succeeded_at"),
    )
}

@Repository
class DonationRepository(
    private val jdbcClient: JdbcClient,
) {
    fun insert(donation: Donation, now: Instant) {
        jdbcClient.sql(
            """
            INSERT INTO donations (id, donor_name, message, show_on_wall, created_at)
            VALUES (:id, :donorName, :message, :showOnWall, :createdAt)
            """,
        )
            .param("id", donation.paymentId)
            .param("donorName", donation.donorName)
            .param("message", donation.message)
            .param("showOnWall", donation.showOnWall)
            .param("createdAt", now.toSql())
            .update()
    }

    fun findByPaymentId(paymentId: UUID): Donation? =
        jdbcClient.sql("SELECT id, donor_name, message, show_on_wall FROM donations WHERE id = :id")
            .param("id", paymentId)
            .query(donationMapper)
            .list()
            .firstOrNull()

    fun findWall(page: PageQuery): List<DonationWallEntry> =
        jdbcClient.sql(
            """
            SELECT p.id, d.donor_name, d.message, p.amount_kopecks, p.succeeded_at
            FROM payments p
            JOIN donations d ON d.id = p.id
            WHERE p.purpose = 'AUTHOR_DONATION' AND p.status = 'SUCCEEDED' AND d.show_on_wall
            ORDER BY p.id DESC
            LIMIT :limit OFFSET :offset
            """,
        )
            .param("limit", page.size)
            .param("offset", page.offset)
            .query(wallEntryMapper)
            .list()
}
