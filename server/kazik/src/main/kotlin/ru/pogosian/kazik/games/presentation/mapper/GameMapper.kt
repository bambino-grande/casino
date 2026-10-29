package ru.pogosian.kazik.games.presentation.mapper

import org.springframework.stereotype.Component
import ru.pogosian.kazik.games.business.rounds.Round
import ru.pogosian.kazik.games.business.services.JackpotInfo
import ru.pogosian.kazik.games.business.services.SpinResult
import ru.pogosian.kazik.games.business.slots.SlotMachine
import ru.pogosian.kazik.games.presentation.DTO.response.JackpotResponse
import ru.pogosian.kazik.games.presentation.DTO.response.JackpotWinResponse
import ru.pogosian.kazik.games.presentation.DTO.response.PrizeInfoResponse
import ru.pogosian.kazik.games.presentation.DTO.response.RoundResponse
import ru.pogosian.kazik.games.presentation.DTO.response.SlotMachineResponse
import ru.pogosian.kazik.games.presentation.DTO.response.SpinResponse
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class GameMapper {
    fun toDto(result: SpinResult): SpinResponse =
        SpinResponse(
            roundId = result.round.id,
            reels = result.round.reels.map { it.name },
            bet = result.round.bet.toDecimal(),
            win = result.round.win.toDecimal(),
            jackpotWin = result.round.jackpotWin.toDecimal(),
            balanceAfter = result.balanceAfter?.toDecimal(),
            animation = result.animation.name,
            replayed = result.replayed,
            prize = result.prize?.let { PrizeInfoResponse(it.id, it.imageUrl) },
            createdAt = result.round.createdAt,
        )

    fun toDto(round: Round): RoundResponse =
        RoundResponse(
            roundId = round.id,
            userId = round.userId.value,
            machineCode = round.machineCode,
            paytableVersion = round.paytableVersion,
            reels = round.reels.map { it.name },
            bet = round.bet.toDecimal(),
            win = round.win.toDecimal(),
            jackpotWin = round.jackpotWin.toDecimal(),
            createdAt = round.createdAt,
        )

    fun toDto(machine: SlotMachine): SlotMachineResponse =
        SlotMachineResponse(
            code = machine.code,
            title = machine.title,
            paytableVersion = machine.paytableVersion,
            minBet = machine.minBet.toDecimal(),
            maxBet = machine.maxBet.toDecimal(),
            reelWeights = machine.reelWeights.mapKeys { it.key.name },
            threeOfKind = machine.threeOfKind.mapKeys { it.key.name },
            twoCherries = machine.twoCherries,
            twoCapybaras = machine.twoCapybaras,
            baseRtp = BigDecimal.valueOf(machine.baseRtp()).setScale(4, RoundingMode.HALF_UP),
        )

    fun toDto(jackpot: JackpotInfo): JackpotResponse =
        JackpotResponse(
            amount = jackpot.pool.amount.toDecimal(),
            seed = jackpot.pool.seed.toDecimal(),
            recentWins = jackpot.recentWins.map { JackpotWinResponse(it.id, it.jackpotWin.toDecimal(), it.createdAt) },
        )
}
