package com.aleixcos.visto.network

import com.aleixcos.visto.network.dto.PlayerDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun signInAnonymously(): Result<PlayerDto> = runCatching {
        // Crear sesión anónima en Supabase Auth
        supabase.auth.signInAnonymously()
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("No user after anonymous sign in")

        // Crear jugador en la tabla players
        val username = generateUsername()
        val avatar = listOf("🦊","🐺","🦁","🐯","🐻","🦝","🐨","🐼").random()

        supabase.postgrest["players"].insert(
            buildJsonObject {
                put("id", userId)
                put("username", username)
                put("avatar", avatar)
                put("rank", 1000)
            }
        )

        PlayerDto(
            id = userId,
            username = username,
            avatar = avatar,
            rank = 1000
        )
    }

    override suspend fun signInWithGoogle(idToken: String): Result<PlayerDto> = runCatching {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
        }
        getCurrentPlayer() ?: error("No player after Google sign in")
    }

    override suspend fun signInWithApple(idToken: String): Result<PlayerDto> = runCatching {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Apple
        }
        getCurrentPlayer() ?: error("No player after Apple sign in")
    }

    override suspend fun getCurrentPlayer(): PlayerDto? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        return runCatching {
            supabase.postgrest["players"]
                .select(Columns.ALL) {
                    filter { eq("id", userId) }
                }
                .decodeSingle<PlayerDto>()
        }.getOrNull()
    }

    override suspend fun isLoggedIn(): Boolean =
        supabase.auth.currentUserOrNull() != null

    private fun generateUsername(): String {
        val animals = listOf("Lion","Fox","Wolf","Bear","Eagle","Tiger","Panda","Koala")
        val number = (1000..9999).random()
        return "${animals.random()}#$number"
    }
}