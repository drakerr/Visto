package com.aleixcos.visto.domain

data class GameBoard(
    val items: List<BoardItem>
) {
    companion object {
        fun empty() = GameBoard(items = emptyList())
    }
}

data class BoardItem(
    val id: Int,
    val imageKey: String,
    val x: Float,           // posición normalizada 0.0-1.0
    val y: Float,           // posición normalizada 0.0-1.0
    val isFound: Boolean = false
)