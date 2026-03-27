package com.aleixcos.visto.network

import com.aleixcos.visto.domain.GhostRun
import com.aleixcos.visto.network.dto.MatchResultDto
import com.aleixcos.visto.network.dto.RunDto
import com.aleixcos.visto.network.dto.toDomain
import com.aleixcos.visto.network.dto.toDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class SupabaseRunRepository(private val client: HttpClient) : RunRepository {

    private val baseUrl = SupabaseConfig.URL
    private val headers: HeadersBuilder.() -> Unit = {
        append("apikey", SupabaseConfig.ANON_KEY)
        append("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
        append("Content-Type", "application/json")
        append("Prefer", "return=representation")
    }

    // Obtener un ghost run para competir — busca uno con score similar
    override suspend fun fetchGhostRun(playerScore: Int): Result<GhostRun> = runCatching {
        val minScore = (playerScore * 0.7).toInt()
        val maxScore = (playerScore * 1.3).toInt()
        val response = client.get("$baseUrl/rest/v1/runs") {
            headers(headers)
            parameter("final_score", "gte.$minScore")
            parameter("final_score", "lte.$maxScore")
            parameter("order", "created_at.desc")
            parameter("limit", "10")
        }
        val runs = response.body<List<RunDto>>()
        runs.random().toDomain()
    }

    // Subir el run propio al servidor
    override suspend fun uploadRun(run: GhostRun): Result<Unit> = runCatching {
        client.post("$baseUrl/rest/v1/runs") {
            headers(headers)
            setBody(run.toDto())
        }
    }

    // Guardar resultado de una partida
    override suspend fun saveMatchResult(
        playerId: String,
        ghostRunId: String,
        playerScore: Int,
        ghostScore: Int,
        outcome: String
    ): Result<Unit> = runCatching {
        client.post("$baseUrl/rest/v1/matches") {
            headers(headers)
            setBody(MatchResultDto(
                playerId = playerId,
                ghostRunId = ghostRunId,
                playerScore = playerScore,
                ghostScore = ghostScore,
                outcome = outcome
            ))
        }
    }
}