package com.aleixcos.visto.powerups

sealed interface PowerUp {
    val id: String
    val emoji: String
    val label: String
    val durationMs: Long  // 0 = instantáneo

    data object Reveal : PowerUp {
        override val id = "reveal"
        override val emoji = "🔍"
        override val label = "Revelar"
        override val durationMs = 3_000L
    }
    data object DoublePoints : PowerUp {
        override val id = "double_points"
        override val emoji = "⭐"
        override val label = "x2 Puntos"
        override val durationMs = 10_000L
    }
    data object FreezeTime : PowerUp {
        override val id = "freeze_time"
        override val emoji = "❄️"
        override val label = "Congelar"
        override val durationMs = 4_000L
    }
    data object Shuffle : PowerUp {
        override val id = "shuffle"
        override val emoji = "🔀"
        override val label = "Mezclar"
        override val durationMs = 0L
    }
    data object ComboShield : PowerUp {
        override val id = "combo_shield"
        override val emoji = "🛡️"
        override val label = "Escudo"
        override val durationMs = 0L  // instantáneo, se consume al fallar
    }
}

// Todas las disponibles en orden de UI
val ALL_POWER_UPS = listOf(
    PowerUp.Reveal,
    PowerUp.DoublePoints,
    PowerUp.FreezeTime,
    PowerUp.Shuffle,
    PowerUp.ComboShield
)