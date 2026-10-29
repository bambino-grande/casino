package ru.pogosian.kazik.games.business.providers

interface RandomSource {
    fun nextInt(bound: Int): Int
}
