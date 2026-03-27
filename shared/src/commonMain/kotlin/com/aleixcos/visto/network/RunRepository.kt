package com.aleixcos.visto.network

import com.aleixcos.visto.domain.GhostRun

interface RunRepository {
    suspend fun fetchGhostRun(playerScore: Int): Result<GhostRun>
    suspend fun uploadRun(run: GhostRun): Result<Unit>
    suspend fun saveMatchResult(
        playerId: String,
        ghostRunId: String,
        playerScore: Int,
        ghostScore: Int,
        outcome: String
    ): Result<Unit>
}