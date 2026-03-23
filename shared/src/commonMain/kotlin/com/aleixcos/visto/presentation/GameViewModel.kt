package com.aleixcos.visto.presentation

import com.aleixcos.visto.domain.GamePhase
import com.aleixcos.visto.domain.GameState
import com.aleixcos.visto.domain.GhostRun
import com.aleixcos.visto.engine.BoardGenerator
import com.aleixcos.visto.engine.GameAction
import com.aleixcos.visto.engine.GameEngine
import com.aleixcos.visto.ghostrun.GhostRunMock
import com.aleixcos.visto.ghostrun.GhostRunRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class GameViewModel {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var gameLoopJob: Job? = null
    private var recorder = GhostRunRecorder()

    // El último run grabado — listo para subir al servidor
    private var lastRecordedRun: GhostRun? = null
    val recordedRun: GhostRun? get() = lastRecordedRun

    private val _state = MutableStateFlow(GameState.initial())
    val state: StateFlow<GameState> = _state.asStateFlow()

    fun startGame(seed: Long = Clock.System.now().toEpochMilliseconds()) {
        val board = BoardGenerator.generate(seed, cols = 5, rows = 8)
        val ghostRun = GhostRunMock.generate(seed = seed, difficulty = 0.5f)
        val baseState = GameState.initial(seed).copy(
            board = board,
            phase = GamePhase.COUNTDOWN,
            ghostRun = ghostRun
        )
        val stateWithTargets = GameEngine.initTargets(baseState)
        _state.value = stateWithTargets
        // Iniciar grabación
        recorder = GhostRunRecorder()
        recorder.start(tick = 0L)

        startCountdown()
    }

    private fun startCountdown() {
        scope.launch {
            delay(3_000L)
            _state.update { it.copy(phase = GamePhase.PLAYING) }
            startGameLoop()
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = scope.launch {
            var lastFrameMs = Clock.System.now().toEpochMilliseconds()
            while (_state.value.phase == GamePhase.PLAYING) {
                delay(16L)
                val now = Clock.System.now().toEpochMilliseconds()
                val delta = now - lastFrameMs
                lastFrameMs = now
                _state.update { GameEngine.tick(it, delta) }
            }
            // Partida terminada — guardar run
            if (_state.value.phase == GamePhase.FINISHED) {
                saveRun()
            }
        }
    }

    fun onAction(action: GameAction) {
        val currentState = _state.value
        _state.update { GameEngine.processAction(it, action) }

        // Grabar la acción
        val newState = _state.value
        when (action) {
            is GameAction.TapItem -> {
                val wasTarget = currentState.activeTargets.any { it.id == action.itemId }
                if (wasTarget) {
                    recorder.recordFind(
                        tick = newState.tickCount,
                        itemId = action.itemId
                    )
                } else {
                    recorder.recordWrongTap(tick = newState.tickCount)
                }
            }
            is GameAction.UsePowerUp -> {
                recorder.recordPowerUp(
                    tick = newState.tickCount,
                    powerUpId = action.powerUpId
                )
            }
            else -> {}
        }
    }

    private fun saveRun() {
        val state = _state.value
        lastRecordedRun = recorder.buildRun(
            runId = "local_${state.seed}",
            playerId = "local_player",
            seed = state.seed,
            finalScore = state.score,
            foundCount = state.foundCount,
            durationMs = 60_000L - state.timeRemainingMs
        )
    }

    fun resetGame() {
        gameLoopJob?.cancel()
        _state.update { GameState.initial() }
    }

    fun onCleared() {
        gameLoopJob?.cancel()
    }
}