package com.aleixcos.visto.scoring

import com.aleixcos.visto.domain.GameResult
import com.aleixcos.visto.domain.GameState
import com.aleixcos.visto.domain.RunEvent

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

    fun buildResult(state: GameState, maxCombo: Int): GameResult {
        val basePoints = state.score
        val totalLocal = basePoints
        val totalGhost = state.ghostSnapshot.currentScore

        val outcome = when {
            totalLocal > totalGhost -> GameResult.Outcome.WIN
            totalLocal < totalGhost -> GameResult.Outcome.LOSE
            else                    -> GameResult.Outcome.DRAW
        }

        // Calcular stats del rival desde sus eventos
        val ghostEvents = state.ghostRun?.events ?: emptyList()
        var ghostCombo = 0
        var ghostMaxCombo = 0
        var ghostWrongTaps = 0
        var ghostFound = 0

        ghostEvents.forEach { event ->
            when (event) {
                is RunEvent.ItemFound -> {
                    ghostCombo++
                    ghostFound++
                    if (ghostCombo > ghostMaxCombo) ghostMaxCombo = ghostCombo
                }
                is RunEvent.WrongTap -> {
                    ghostCombo = 0
                    ghostWrongTaps++
                }
                is RunEvent.PowerUpUsed -> { /* no afecta al cálculo del resultado */ }
            }
        }

        val ghostTotal = ghostFound + ghostWrongTaps
        val ghostAccuracy = if (ghostTotal > 0)
            (ghostFound.toFloat() / ghostTotal * 100).toInt() else 0

        val localTotal = state.foundCount + state.wrongTapCount
        val localAccuracy = if (localTotal > 0)
            (state.foundCount.toFloat() / localTotal * 100).toInt() else 0

        return GameResult(
            outcome = outcome,
            localScore = totalLocal,
            ghostScore = totalGhost,
            localFound = state.foundCount,
            ghostFound = ghostFound,
            maxCombo = maxCombo,
            ghostMaxCombo = ghostMaxCombo,
            ghostWrongTaps = ghostWrongTaps,
            ghostAccuracy = ghostAccuracy,
            basePoints = basePoints,
            wrongTapCount = state.wrongTapCount,
            durationMs = 60_000L - state.timeRemainingMs,
            rivalUsername = "Player_${state.seed % 9999}",
            rivalAvatar = listOf("🦊","🐺","🦁","🐯","🐻","🦝","🐨","🐼").random(),
            rivalRank = (100..9999).random()
        )
    }
}