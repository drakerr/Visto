package com.aleixcos.visto.ghostrun

import com.aleixcos.visto.domain.GhostRun
import com.aleixcos.visto.domain.GhostRunSnapshot
import com.aleixcos.visto.domain.RunEvent
import com.aleixcos.visto.scoring.ScoreCalculator

object GhostRunPlayer {

    fun snapshotAt(run: GhostRun, tick: Long): GhostRunSnapshot {
        val eventsUpToTick = run.events.filter { it.tick <= tick }
        var score = 0
        var combo = 0
        var foundCount = 0

        eventsUpToTick.forEach { event ->
            when (event) {
                is RunEvent.ItemFound -> {
                    combo++
                    foundCount++
                    score += ScoreCalculator.pointsForFind(combo)
                }
                is RunEvent.WrongTap -> {
                    combo = 0
                }
            }
        }

        return GhostRunSnapshot(
            currentScore = score,
            foundCount = foundCount,
            lastEventTick = eventsUpToTick.lastOrNull()?.tick ?: 0L
        )
    }
}