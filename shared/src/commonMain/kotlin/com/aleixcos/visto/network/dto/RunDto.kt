package com.aleixcos.visto.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RunDto(
    val id: String,
    @SerialName("player_id") val playerId: String,
    val seed: Long,
    @SerialName("final_score") val finalScore: Int,
    @SerialName("found_count") val foundCount: Int,
    @SerialName("duration_ms") val durationMs: Long,
    val events: List<RunEventDto>
)

@Serializable
data class RunEventDto(
    val type: String,       // "find", "wrong", "powerup"
    val tick: Long,
    @SerialName("item_id") val itemId: Int? = null,
    @SerialName("powerup_id") val powerUpId: String? = null
)

@Serializable
data class PlayerDto(
    val id: String,
    val username: String,
    val avatar: String,
    val rank: Int
)

@Serializable
data class MatchResultDto(
    @SerialName("player_id") val playerId: String,
    @SerialName("ghost_run_id") val ghostRunId: String,
    @SerialName("player_score") val playerScore: Int,
    @SerialName("ghost_score") val ghostScore: Int,
    val outcome: String
)