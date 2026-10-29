package ru.pogosian.kazik.rewards.business.exceptions

import ru.pogosian.kazik.common.business.exceptions.ConflictException
import ru.pogosian.kazik.common.business.exceptions.DomainValidationException
import ru.pogosian.kazik.common.business.exceptions.ExternalServiceUnavailableException
import ru.pogosian.kazik.common.business.exceptions.NotFoundException
import java.util.UUID

class InvalidPrizeException(message: String) : DomainValidationException(message, "INVALID_PRIZE")

class PrizeNotFoundException(id: UUID) : NotFoundException("Prize $id does not exist", "PRIZE_NOT_FOUND")

class PrizeOwnerMismatchException(roundId: UUID) :
    ConflictException("Prize for round $roundId belongs to another user", "PRIZE_OWNER_MISMATCH")

class NoPrizeImagesException(directory: String) :
    ExternalServiceUnavailableException("There are no prize images in $directory", "NO_PRIZE_IMAGES")

class PrizeImageMissingException(fileName: String) :
    ExternalServiceUnavailableException("Prize image '$fileName' is missing in the storage", "PRIZE_IMAGE_MISSING")
