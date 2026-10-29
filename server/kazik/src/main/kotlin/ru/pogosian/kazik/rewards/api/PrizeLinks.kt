package ru.pogosian.kazik.rewards.api

import java.util.UUID

fun prizeImagePath(prizeId: UUID): String = "/api/v1/rewards/prizes/$prizeId/image"
