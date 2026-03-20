package com.aleixcos.visto.domain

import kotlinx.datetime.Clock

data class GameState(
    val board: GameBoard,
    val activeTargets: List<BoardItem>,     // 3-5 objetivos visibles
    val targetQueue: List<BoardItem>,       // próximos objetivos
    val foundCount: Int,
    val phase: GamePhase,
    val timeRemainingMs: Long,
    val score: Int,
    val combo: Int,
    val lastFoundMs: Long,
    val seed: Long,
    val tickCount: Long = 0L,
    val wrongTapCount: Int = 0
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
}

enum class GamePhase { COUNTDOWN, PLAYING, FINISHED }