package com.aleixcos.visto.domain

data class GameResult(
    val outcome: Outcome,
    val localScore: Int,
    val ghostScore: Int,
    val localFound: Int,
    val ghostFound: Int,
    val maxCombo: Int,
    val ghostMaxCombo: Int,
    val ghostWrongTaps: Int,
    val ghostAccuracy: Int,
    val basePoints: Int,
    val wrongTapCount: Int = 0,
    val durationMs: Long,
    val rivalUsername: String,
    val rivalAvatar: String,
    val rivalRank: Int
) {
    enum class Outcome { WIN, LOSE, DRAW }
}