package com.aleixcos.visto.domain

data class ActivePowerUp(
    val powerUpId: String,
    val remainingMs: Long
)

// Power-ups disponibles para usar (cargas)
data class PowerUpCharge(
    val powerUpId: String,
    val charges: Int
)