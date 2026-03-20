package com.aleixcos.visto.ghostrun

import com.aleixcos.visto.domain.GhostRun
import com.aleixcos.visto.domain.RunEvent

class GhostRunRecorder {
    private val events = mutableListOf<RunEvent>()
    private var startTick: Long = 0L

    fun start(tick: Long) { startTick = tick }

    fun recordFind(tick: Long, itemId: Int) {
        events.add(RunEvent.ItemFound(tick = tick, itemId = itemId))
    }

    fun recordWrongTap(tick: Long) {
        events.add(RunEvent.WrongTap(tick = tick))
    }

    fun buildRun(
        runId: String,
        playerId: String,
        seed: Long,
        finalScore: Int,
        foundCount: Int,
        durationMs: Long
    ): GhostRun = GhostRun(
        runId = runId,
        playerId = playerId,
        seed = seed,
        events = events.toList(),
        finalScore = finalScore,
        foundCount = foundCount,
        durationMs = durationMs
    )
}