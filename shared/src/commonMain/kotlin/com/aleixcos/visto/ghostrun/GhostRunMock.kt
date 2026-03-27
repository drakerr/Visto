package com.aleixcos.visto.ghostrun

import com.aleixcos.visto.domain.GhostRun
import com.aleixcos.visto.domain.RunEvent
import kotlin.random.Random

object GhostRunMock {

    // Genera un rival con dificultad ajustable
    // difficulty: 0.0 (muy fácil) → 1.0 (muy difícil)
    fun generate(seed: Long, difficulty: Float = 0.5f): GhostRun {
        val random = Random(seed + 999) // seed diferente al tablero

        // Velocidad del rival: cada cuántos ticks encuentra un objeto
        // A 60fps (tick = 16ms), 60 ticks = ~1 segundo
        val baseTicksPerFind = when {
            difficulty < 0.3f -> 180L  // lento: ~3s por objeto
            difficulty < 0.6f -> 120L  // medio: ~2s por objeto
            difficulty < 0.8f -> 80L   // rápido: ~1.3s por objeto
            else              -> 55L   // muy rápido: ~0.9s por objeto
        }

        val events = mutableListOf<RunEvent>()
        var currentTick = 60L // empieza a los ~1s
        val totalDurationTicks = 3750L // 60 segundos a 60fps

        while (currentTick < totalDurationTicks) {
            // Variación aleatoria en la velocidad para que no sea robótico
            val variation = (random.nextFloat() - 0.5f) * baseTicksPerFind * 0.4f
            val ticksUntilNext = (baseTicksPerFind + variation).toLong().coerceAtLeast(30L)

            // A veces falla (wrong tap)
            val mistakeChance = when {
                difficulty < 0.3f -> 0.25f
                difficulty < 0.6f -> 0.12f
                else              -> 0.05f
            }
            if (random.nextFloat() < mistakeChance) {
                events.add(RunEvent.WrongTap(tick = currentTick))
                currentTick += 20L
            }

            currentTick += ticksUntilNext
            if (currentTick < totalDurationTicks) {
                events.add(RunEvent.ItemFound(tick = currentTick, itemId = events.size))
            }
        }

        // Calcular score final del rival
        var score = 0
        var combo = 0
        var foundCount = 0
        events.forEach { event ->
            when (event) {
                is RunEvent.ItemFound -> { combo++; foundCount++; score += 100 * minOf(combo / 3 + 1, 3) }
                is RunEvent.WrongTap  -> combo = 0
                is RunEvent.PowerUpUsed -> { /* no afecta al score del mock */ }
            }
        }

        return GhostRun(
            runId = "mock_$seed",
            playerId = "ghost_rival",
            seed = seed,
            events = events,
            finalScore = score,
            foundCount = foundCount,
            durationMs = 60_000L
        )
    }
}