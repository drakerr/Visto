package com.aleixcos.visto.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object SupabaseConfig {
    const val URL = "https://qegjtxgyjvinowgotgkb.supabase.co"
    const val ANON_KEY = "sb_publishable_5kTzeYtChix8uUSu4rYjOA_yEt5e1lZ"
}