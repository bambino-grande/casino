package ru.pogosian.kazik.games.business.exceptions

import ru.pogosian.kazik.common.business.exceptions.BusinessRuleException
import ru.pogosian.kazik.common.business.exceptions.ConflictException
import ru.pogosian.kazik.common.business.exceptions.DomainValidationException
import ru.pogosian.kazik.common.business.exceptions.NotFoundException
import java.util.UUID

class InvalidBetException(message: String) : DomainValidationException(message, "INVALID_BET")

class SlotMachineNotFoundException(code: String) : NotFoundException("Slot machine '$code' does not exist", "SLOT_MACHINE_NOT_FOUND")

class RoundNotFoundException(id: UUID) : NotFoundException("Round $id does not exist", "ROUND_NOT_FOUND")

class InsufficientNaeBallsException : BusinessRuleException("Not enough naeballs for this bet, top up the wallet", "INSUFFICIENT_FUNDS")

class DuplicateRoundException :
    ConflictException("Round with this Idempotency-Key is being processed, retry later", "ROUND_IN_PROGRESS")
