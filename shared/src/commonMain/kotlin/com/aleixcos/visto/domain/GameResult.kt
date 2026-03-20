package com.aleixcos.visto.domain

data class GameResult(
    val localScore: Int,
    val ghostScore: Int,
    val outcome: Outcome,
    val matchesFound: Int,
    val durationMs: Long,
    val maxCombo: Int
) {
    enum class Outcome { WIN, LOSE, DRAW }
}