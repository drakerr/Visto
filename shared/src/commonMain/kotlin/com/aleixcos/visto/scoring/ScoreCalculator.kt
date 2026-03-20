package com.aleixcos.visto.scoring

object ScoreCalculator {

    private const val BASE_POINTS = 100

    fun pointsForFind(combo: Int): Int {
        val multiplier = when {
            combo >= 5 -> 3
            combo >= 3 -> 2
            else       -> 1
        }
        return BASE_POINTS * multiplier
    }

    fun calculateFinal(score: Int, timeRemainingMs: Long, foundCount: Int): Int {
        val timeBonus = (timeRemainingMs / 1000).toInt() * 5
        val foundBonus = foundCount * 10
        return score + timeBonus + foundBonus
    }
}