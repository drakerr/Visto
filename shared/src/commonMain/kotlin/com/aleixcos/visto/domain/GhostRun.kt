package com.aleixcos.visto.domain

data class GhostRun(
    val runId: String,
    val playerId: String,
    val seed: Long,
    val events: List<RunEvent>,
    val finalScore: Int,
    val foundCount: Int,
    val durationMs: Long
)

sealed interface RunEvent {
    val tick: Long
    data class ItemFound(
        override val tick: Long,
        val itemId: Int
    ) : RunEvent
    data class WrongTap(
        override val tick: Long
    ) : RunEvent
}

data class GhostRunSnapshot(
    val currentScore: Int = 0,
    val foundCount: Int = 0,
    val lastEventTick: Long = 0L
)