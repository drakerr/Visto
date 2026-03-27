package com.aleixcos.visto.network.dto

import com.aleixcos.visto.domain.GhostRun
import com.aleixcos.visto.domain.RunEvent

fun GhostRun.toDto(): RunDto = RunDto(
    id = runId,
    playerId = playerId,
    seed = seed,
    finalScore = finalScore,
    foundCount = foundCount,
    durationMs = durationMs,
    events = events.map { it.toDto() }
)

fun RunEvent.toDto(): RunEventDto = when (this) {
    is RunEvent.ItemFound   -> RunEventDto(type = "find", tick = tick, itemId = itemId)
    is RunEvent.WrongTap    -> RunEventDto(type = "wrong", tick = tick)
    is RunEvent.PowerUpUsed -> RunEventDto(type = "powerup", tick = tick, powerUpId = powerUpId)
}

fun RunDto.toDomain(): GhostRun = GhostRun(
    runId = id,
    playerId = playerId,
    seed = seed,
    events = events.map { it.toDomain() },
    finalScore = finalScore,
    foundCount = foundCount,
    durationMs = durationMs
)

fun RunEventDto.toDomain(): RunEvent = when (type) {
    "find"    -> RunEvent.ItemFound(tick = tick, itemId = itemId ?: 0)
    "wrong"   -> RunEvent.WrongTap(tick = tick)
    "powerup" -> RunEvent.PowerUpUsed(tick = tick, powerUpId = powerUpId ?: "")
    else      -> RunEvent.WrongTap(tick = tick)
}