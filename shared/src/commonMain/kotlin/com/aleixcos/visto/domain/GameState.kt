package com.aleixcos.visto.domain

import com.aleixcos.visto.domain.ActivePowerUp
import com.aleixcos.visto.domain.PowerUpCharge
import kotlinx.datetime.Clock

data class GameState(
    val board: GameBoard,
    val activeTargets: List<BoardItem>,
    val targetQueue: List<BoardItem>,
    val foundCount: Int,
    val phase: GamePhase,
    val timeRemainingMs: Long,
    val score: Int,
    val combo: Int,
    val lastFoundMs: Long,
    val seed: Long,
    val tickCount: Long = 0L,
    val wrongTapCount: Int = 0,
    val ghostRun: GhostRun? = null,
    val ghostSnapshot: GhostRunSnapshot = GhostRunSnapshot(),
    // Power-ups
    val activePowerUps: List<ActivePowerUp> = emptyList(),   // activos con duración
    val powerUpCharges: List<PowerUpCharge> = emptyList(),   // cargas disponibles
    val hasComboShield: Boolean = false,                      // escudo activo
    val revealedItemId: Int? = null,                         // item resaltado por Reveal
    val isTimeFrozen: Boolean = false,
    val maxCombo: Int = 0,
    val result: GameResult? = null// timer pausado
) {
    companion object {
        fun initial(seed: Long = Clock.System.now().toEpochMilliseconds()): GameState = GameState(
            board = GameBoard.empty(),
            activeTargets = emptyList(),
            targetQueue = emptyList(),
            foundCount = 0,
            phase = GamePhase.COUNTDOWN,
            timeRemainingMs = 60_000L,
            score = 0,
            combo = 0,
            lastFoundMs = 0L,
            seed = seed
        )
    }

    // Helpers
    fun isDoublePointsActive() = activePowerUps.any { it.powerUpId == "double_points" }
    fun isRevealActive() = activePowerUps.any { it.powerUpId == "reveal" }
    fun isTimeFrozenActive() = activePowerUps.any { it.powerUpId == "freeze_time" }
    fun chargesFor(powerUpId: String) = powerUpCharges.find { it.powerUpId == powerUpId }?.charges ?: 0
}

enum class GamePhase { COUNTDOWN, PLAYING, FINISHED }