package ru.pogosian.kazik.common.business

import ru.pogosian.kazik.common.business.exceptions.DomainValidationException

private const val MAX_PAGE_SIZE = 100

data class PageQuery(val page: Int, val size: Int) {
    init {
        if (page < 0) throw DomainValidationException("page must not be negative", "INVALID_PAGE")
        if (size !in 1..MAX_PAGE_SIZE) throw DomainValidationException("size must be in 1..$MAX_PAGE_SIZE", "INVALID_PAGE")
    }

    val offset: Long get() = page.toLong() * size
}
