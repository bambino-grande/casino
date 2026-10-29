package ru.pogosian.kazik.wallet.business.exceptions

import ru.pogosian.kazik.common.business.UserId
import ru.pogosian.kazik.common.business.exceptions.BusinessRuleException
import ru.pogosian.kazik.common.business.exceptions.DomainValidationException

class UnbalancedTransferException(message: String) : DomainValidationException(message, "UNBALANCED_TRANSFER")

class InsufficientFundsException(val userId: UserId) :
    BusinessRuleException("Not enough naeballs on the balance of user $userId", "INSUFFICIENT_FUNDS")
