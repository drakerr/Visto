package com.aleixcos.visto.network

import com.aleixcos.visto.network.dto.PlayerDto

interface AuthRepository {
    suspend fun signInAnonymously(): Result<PlayerDto>
    suspend fun signInWithGoogle(idToken: String): Result<PlayerDto>
    suspend fun signInWithApple(idToken: String): Result<PlayerDto>
    suspend fun getCurrentPlayer(): PlayerDto?
    suspend fun isLoggedIn(): Boolean
}