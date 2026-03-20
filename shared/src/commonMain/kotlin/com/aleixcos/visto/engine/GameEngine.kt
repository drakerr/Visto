package com.aleixcos.visto.engine

import com.aleixcos.visto.domain.BoardItem
import com.aleixcos.visto.domain.GamePhase
import com.aleixcos.visto.domain.GameState
import com.aleixcos.visto.scoring.ScoreCalculator

object GameEngine {

    private const val ACTIVE_TARGETS = 4

    fun tick(state: GameState, deltaMs: Long): GameState {
        if (state.phase != GamePhase.PLAYING) return state
        val newTime = (state.timeRemainingMs - deltaMs).coerceAtLeast(0L)
        return state.copy(
            timeRemainingMs = newTime,
            tickCount = state.tickCount + 1,
            phase = if (newTime <= 0L) GamePhase.FINISHED else GamePhase.PLAYING
        )
    }

    fun processAction(state: GameState, action: GameAction): GameState {
        if (state.phase != GamePhase.PLAYING) return state
        return when (action) {
            is GameAction.TapItem    -> handleTap(state, action)
            is GameAction.UsePowerUp -> state
            is GameAction.Surrender  -> state.copy(phase = GamePhase.FINISHED)
        }
    }

    private fun handleTap(state: GameState, action: GameAction.TapItem): GameState {
        val tappedItem = state.board.items.find { it.id == action.itemId } ?: return state
        val isTarget = state.activeTargets.any { it.id == tappedItem.id }
        return if (isTarget) handleCorrectTap(state, tappedItem)
        else handleWrongTap(state)
    }

    private fun handleCorrectTap(state: GameState, item: BoardItem): GameState {
        // Reemplazar el item encontrado por uno nuevo en la misma posición
        val currentKeys = state.board.items.map { it.imageKey }.toSet()
        val newEmoji = BoardGenerator.randomEmoji(
            seed = state.seed + state.foundCount,
            excludeKeys = currentKeys
        )
        val newItem = item.copy(imageKey = newEmoji, isFound = false)
        val newItems = state.board.items.map {
            if (it.id == item.id) newItem else it
        }

        // Actualizar targets — quitar el encontrado, añadir el nuevo item como candidato futuro
        val newTargets = state.activeTargets.filter { it.id != item.id }
        val nextTarget = state.targetQueue.firstOrNull()
        val newQueue = if (nextTarget != null) state.targetQueue.drop(1) else state.targetQueue
        val filledTargets = if (nextTarget != null) newTargets + nextTarget else newTargets

        val newCombo = state.combo + 1
        val points = ScoreCalculator.pointsForFind(combo = newCombo)

        return state.copy(
            board = state.board.copy(items = newItems),
            activeTargets = filledTargets,
            targetQueue = newQueue + newItem, // el nuevo item entra a la cola de futuros targets
            foundCount = state.foundCount + 1,
            score = state.score + points,
            combo = newCombo,
            lastFoundMs = state.timeRemainingMs
        )
    }

    private fun handleWrongTap(state: GameState): GameState {
        return state.copy(combo = 0, wrongTapCount = state.wrongTapCount + 1)
    }

    fun initTargets(state: GameState): GameState {
        val items = state.board.items.shuffled()
        val active = items.take(ACTIVE_TARGETS)
        val queue = items.drop(ACTIVE_TARGETS)
        return state.copy(activeTargets = active, targetQueue = queue)
    }
}