package ru.pogosian.kazik.games.business.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.pogosian.kazik.common.business.IdempotencyKey
import ru.pogosian.kazik.common.business.NaeBalls
import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.ZERO_NAE_BALLS
import ru.pogosian.kazik.common.business.exceptions.IdempotencyConflictException
import ru.pogosian.kazik.common.business.uuidV7
import ru.pogosian.kazik.games.business.jackpot.JackpotPolicy
import ru.pogosian.kazik.games.business.providers.GrantedPrize
import ru.pogosian.kazik.games.business.providers.RandomSource
import ru.pogosian.kazik.games.business.providers.RewardsGateway
import ru.pogosian.kazik.games.business.providers.WalletGateway
import ru.pogosian.kazik.games.business.rounds.Round
import ru.pogosian.kazik.games.business.rounds.betReference
import ru.pogosian.kazik.games.business.rounds.winReference
import ru.pogosian.kazik.games.business.slots.SlotMachine
import ru.pogosian.kazik.games.business.slots.SlotMachineCatalog
import ru.pogosian.kazik.games.business.slots.SpinOutcome
import ru.pogosian.kazik.games.infrastructure.repository.JackpotPoolRepository
import ru.pogosian.kazik.games.infrastructure.repository.RoundRepository
import java.time.Clock

@Service
class SpinServiceImpl(
    private val catalog: SlotMachineCatalog,
    private val roundRepository: RoundRepository,
    private val jackpotPoolRepository: JackpotPoolRepository,
    private val jackpotPolicy: JackpotPolicy,
    private val walletGateway: WalletGateway,
    private val rewardsGateway: RewardsGateway,
    private val randomSource: RandomSource,
    private val clock: Clock,
) : SpinService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun spin(userId: UserId, machineCode: String, bet: NaeBalls, idempotencyKey: IdempotencyKey): SpinResult {
        val machine = catalog.get(machineCode)
        machine.validateBet(bet)
        roundRepository.findByUserAndKey(userId, idempotencyKey)?.let { return replay(it, machine, bet) }

        val now = clock.instant()
        val roundId = uuidV7(now)
        val balanceAfterBet = walletGateway.debitBet(userId, bet, betReference(roundId))
        val reels = machine.spin(randomSource)
        val outcome = machine.evaluate(reels)
        val jackpotWin = playJackpot(bet, outcome)
        val win = bet * outcome.multiplier + jackpotWin
        val balance = if (win.isPositive()) walletGateway.creditWin(userId, win, winReference(roundId)) else balanceAfterBet

        val round = roundRepository.insert(Round(roundId, userId, idempotencyKey, machine.code, machine.paytableVersion, bet, reels, win, jackpotWin, now))
        log.info("[GAMES] round {} user={} bet={} reels={} win={}", round.id, userId, bet, reels, win)
        return SpinResult(round, balance, machine.animationFor(outcome), false, prizeFor(round))
    }

    private fun replay(round: Round, machine: SlotMachine, bet: NaeBalls): SpinResult {
        if (!round.isSameRequest(machine.code, bet)) throw IdempotencyConflictException(round.idempotencyKey.value)
        log.info("[GAMES] replay of round {}", round.id)
        return SpinResult(round, null, machine.animationFor(machine.evaluate(round.reels)), true, prizeFor(round))
    }

    private fun playJackpot(bet: NaeBalls, outcome: SpinOutcome): NaeBalls {
        val pool = jackpotPoolRepository.lockMain().contribute(jackpotPolicy.contributionFrom(bet))
        if (!outcome.jackpot) {
            jackpotPoolRepository.save(pool)
            return ZERO_NAE_BALLS
        }
        jackpotPoolRepository.save(pool.reset())
        log.info("[GAMES] JACKPOT amount={}", pool.amount)
        return pool.amount
    }

    private fun prizeFor(round: Round): GrantedPrize? =
        if (round.jackpotWin.isPositive()) rewardsGateway.grantJackpotPrize(round.userId, round.id) else null
}
