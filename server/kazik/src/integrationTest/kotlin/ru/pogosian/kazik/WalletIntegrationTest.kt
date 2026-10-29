package ru.pogosian.kazik

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DuplicateKeyException
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.naeBallsOf
import ru.pogosian.kazik.wallet.api.AccountRef
import ru.pogosian.kazik.wallet.api.Leg
import ru.pogosian.kazik.wallet.api.SystemAccount
import ru.pogosian.kazik.wallet.api.TransferCommand
import ru.pogosian.kazik.wallet.api.TransferKind
import ru.pogosian.kazik.wallet.api.TransferResult
import ru.pogosian.kazik.wallet.business.exceptions.InsufficientFundsException
import ru.pogosian.kazik.wallet.business.exceptions.UnbalancedTransferException
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class WalletIntegrationTest(support: IntegrationTestSupport) : BaseIntegrationTest(support) {
    private fun bet(userId: UserId, amount: Long, reference: String) =
        TransferCommand(
            reference,
            TransferKind.BET,
            listOf(Leg(AccountRef.User(userId), naeBallsOf(-amount)), Leg(AccountRef.System(SystemAccount.HOUSE), naeBallsOf(amount))),
        )

    private fun betOrNull(user: UserId): TransferResult? =
        try {
            transferService.transfer(bet(user, 10, "test-bet:${UUID.randomUUID()}"))
        } catch (exception: InsufficientFundsException) {
            null
        }

    @Test
    fun fiftyParallelBetsNeverOverdrawTheBalance() {
        val user = newUser()
        fund(user, 100)

        val pool = Executors.newFixedThreadPool(16)
        val results = pool.invokeAll((1..50).map { Callable { betOrNull(user) } }).map { it.get() }
        pool.shutdown()

        assertEquals(10, results.count { it != null })
        assertEquals(40, results.count { it == null })
        assertEquals(0L, balanceOf(user))
    }

    @Test
    fun ledgerAlwaysBalancesAndCachedBalanceMatchesEntries() {
        val user = newUser()
        fund(user, 30)
        transferService.transfer(bet(user, 7, "test-bet:${UUID.randomUUID()}"))

        val total = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount_minor), 0) FROM ledger_entries", Long::class.java)
        assertEquals(0L, total)
        val fromEntries = jdbcTemplate.queryForObject(
            "SELECT SUM(e.amount_minor) FROM ledger_entries e JOIN accounts a ON a.id = e.account_id WHERE a.owner_id = ?",
            Long::class.java,
            user.value,
        )
        assertEquals(balanceOf(user), fromEntries)
    }

    @Test
    fun sameReferenceCannotBeChargedTwice() {
        val user = newUser()
        fund(user, 50)
        val command = bet(user, 10, "test-bet:${UUID.randomUUID()}")

        transferService.transfer(command)
        assertThrows<DuplicateKeyException> { transferService.transfer(command) }

        assertEquals(naeBallsOf(40).minor, balanceOf(user))
    }

    @Test
    fun unbalancedTransferIsRejectedBeforeAnythingIsWritten() {
        val user = newUser()
        fund(user, 50)
        val reference = "test-bet:${UUID.randomUUID()}"
        val command = TransferCommand(
            reference,
            TransferKind.BET,
            listOf(Leg(AccountRef.User(user), naeBallsOf(-10)), Leg(AccountRef.System(SystemAccount.HOUSE), naeBallsOf(9))),
        )

        assertThrows<UnbalancedTransferException> { transferService.transfer(command) }

        assertEquals(naeBallsOf(50).minor, balanceOf(user))
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ledger_transactions WHERE reference = ?", Int::class.java, reference))
    }
}
