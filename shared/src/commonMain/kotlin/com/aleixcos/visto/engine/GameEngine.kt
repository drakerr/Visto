package com.aleixcos.visto.engine

import com.aleixcos.visto.domain.*
import com.aleixcos.visto.ghostrun.GhostRunPlayer
import com.aleixcos.visto.powerups.PowerUp
import com.aleixcos.visto.scoring.ScoreCalculator
import kotlin.random.Random

object GameEngine {

    private const val ACTIVE_TARGETS = 4

    fun tick(state: GameState, deltaMs: Long): GameState {
        if (state.phase != GamePhase.PLAYING) return state

        // Si el tiempo está congelado no descontamos
        val newTime = if (state.isTimeFrozenActive()) {
            state.timeRemainingMs
        } else {
            (state.timeRemainingMs - deltaMs).coerceAtLeast(0L)
        }

        val newTick = state.tickCount + 1

        // Actualizar power-ups activos — descontar duración
        val updatedPowerUps = state.activePowerUps
            .map { it.copy(remainingMs = it.remainingMs - deltaMs) }
            .filter { it.remainingMs > 0 }

        // Reveal expira — limpiar item resaltado
        val newRevealedId = if (updatedPowerUps.any { it.powerUpId == "reveal" })
            state.revealedItemId else null

        // Ghost
        val newGhostSnapshot = state.ghostRun?.let {
            GhostRunPlayer.snapshotAt(it, newTick)
        } ?: state.ghostSnapshot

        return state.copy(
            timeRemainingMs = newTime,
            tickCount = newTick,
            ghostSnapshot = newGhostSnapshot,
            activePowerUps = updatedPowerUps,
            revealedItemId = newRevealedId,
            phase = if (newTime <= 0L) GamePhase.FINISHED else GamePhase.PLAYING
        )
    }

    fun processAction(state: GameState, action: GameAction): GameState {
        if (state.phase != GamePhase.PLAYING) return state
        return when (action) {
            is GameAction.TapItem    -> handleTap(state, action)
            is GameAction.UsePowerUp -> handlePowerUp(state, action)
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
        val currentKeys = state.board.items.map { it.imageKey }.toSet()
        val newEmoji = BoardGenerator.randomEmoji(
            seed = state.seed + state.foundCount,
            excludeKeys = currentKeys
        )
        val newItem = item.copy(imageKey = newEmoji, isFound = false)
        val newItems = state.board.items.map {
            if (it.id == item.id) newItem else it
        }

        val newTargets = state.activeTargets.filter { it.id != item.id }
        val nextTarget = state.targetQueue.firstOrNull()
        val newQueue = if (nextTarget != null) state.targetQueue.drop(1) else state.targetQueue
        val filledTargets = if (nextTarget != null) newTargets + nextTarget else newTargets

        val newCombo = state.combo + 1

        // Score con multiplicador si double points activo
        val multiplier = if (state.isDoublePointsActive()) 2 else 1
        val points = ScoreCalculator.pointsForFind(combo = newCombo) * multiplier

        // Ganar cargas de power-up por combo
        val newCharges = grantPowerUpCharges(state.powerUpCharges, newCombo)

        return state.copy(
            board = state.board.copy(items = newItems),
            activeTargets = filledTargets,
            targetQueue = newQueue + newItem,
            foundCount = state.foundCount + 1,
            score = state.score + points,
            combo = newCombo,
            lastFoundMs = state.timeRemainingMs,
            powerUpCharges = newCharges,
            revealedItemId = if (state.revealedItemId == item.id) null else state.revealedItemId
        )
    }

    private fun handleWrongTap(state: GameState): GameState {
        return if (state.hasComboShield) {
            // El escudo absorbe el fallo
            state.copy(hasComboShield = false, wrongTapCount = state.wrongTapCount + 1)
        } else {
            state.copy(combo = 0, wrongTapCount = state.wrongTapCount + 1)
        }
    }

    private fun handlePowerUp(state: GameState, action: GameAction.UsePowerUp): GameState {
        val charges = state.chargesFor(action.powerUpId)
        if (charges <= 0) return state

        // Consumir una carga
        val newCharges = state.powerUpCharges.map {
            if (it.powerUpId == action.powerUpId) it.copy(charges = it.charges - 1)
            else it
        }.filter { it.charges > 0 }

        return when (action.powerUpId) {
            "reveal"       -> applyReveal(state, newCharges)
            "double_points"-> applyDuration(state, newCharges, "double_points", 10_000L)
            "freeze_time"  -> applyDuration(state, newCharges, "freeze_time", 4_000L)
            "shuffle"      -> applyShuffle(state, newCharges)
            "combo_shield" -> state.copy(powerUpCharges = newCharges, hasComboShield = true)
            else           -> state
        }
    }

    private fun applyReveal(state: GameState, newCharges: List<PowerUpCharge>): GameState {
        // Resalta el primer objetivo activo que esté en el tablero
        val targetToReveal = state.activeTargets.firstOrNull()
        val itemToReveal = targetToReveal?.let {
            state.board.items.find { item -> item.id == it.id }
        }
        return state.copy(
            powerUpCharges = newCharges,
            activePowerUps = state.activePowerUps + ActivePowerUp("reveal", 3_000L),
            revealedItemId = itemToReveal?.id
        )
    }

    private fun applyDuration(
        state: GameState,
        newCharges: List<PowerUpCharge>,
        id: String,
        durationMs: Long
    ): GameState {
        val existing = state.activePowerUps.filter { it.powerUpId != id }
        return state.copy(
            powerUpCharges = newCharges,
            activePowerUps = existing + ActivePowerUp(id, durationMs)
        )
    }

    private fun applyShuffle(state: GameState, newCharges: List<PowerUpCharge>): GameState {
        val random = Random(state.seed + state.tickCount)
        val shuffledItems = state.board.items.shuffled(random)
        // Mantener ids pero mezclar posiciones
        val newItems = state.board.items.mapIndexed { index, item ->
            item.copy(imageKey = shuffledItems[index].imageKey)
        }
        return state.copy(
            powerUpCharges = newCharges,
            board = state.board.copy(items = newItems)
        )
    }

    // Otorgar cargas según combo alcanzado
    private fun grantPowerUpCharges(
        current: List<PowerUpCharge>,
        combo: Int
    ): List<PowerUpCharge> {
        val powerUpToGrant = when (combo) {
            3    -> "combo_shield"
            5    -> "freeze_time"
            7    -> "reveal"
            10   -> "double_points"
            else -> return current
        }
        val existing = current.find { it.powerUpId == powerUpToGrant }
        return if (existing != null) {
            current.map {
                if (it.powerUpId == powerUpToGrant)
                    it.copy(charges = minOf(it.charges + 1, 3))
                else it
            }
        } else {
            current + PowerUpCharge(powerUpToGrant, 1)
        }
    }

    fun initTargets(state: GameState): GameState {
        val items = state.board.items.shuffled()
        val active = items.take(ACTIVE_TARGETS)
        val queue = items.drop(ACTIVE_TARGETS)
        return state.copy(activeTargets = active, targetQueue = queue)
    }
}