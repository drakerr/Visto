package com.aleixcos.visto

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform