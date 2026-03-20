package com.aleixcos.visto.engine

sealed interface GameAction {
    data class TapItem(val itemId: Int) : GameAction
    data class UsePowerUp(val powerUpId: String) : GameAction
    data object Surrender : GameAction
}