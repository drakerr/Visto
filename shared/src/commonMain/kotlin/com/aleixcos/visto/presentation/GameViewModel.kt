package com.aleixcos.visto.presentation

import com.aleixcos.visto.domain.GamePhase
import com.aleixcos.visto.domain.GameState
import com.aleixcos.visto.engine.BoardGenerator
import com.aleixcos.visto.engine.GameAction
import com.aleixcos.visto.engine.GameEngine
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

    private val _state = MutableStateFlow(GameState.initial())
    val state: StateFlow<GameState> = _state.asStateFlow()

    fun startGame(seed: Long = Clock.System.now().toEpochMilliseconds()) {
        val board = BoardGenerator.generate(seed, cols = 5, rows = 8)
        val baseState = GameState.initial(seed).copy(
            board = board,
            phase = GamePhase.PLAYING
        )
        val stateWithTargets = GameEngine.initTargets(baseState)
        _state.value = stateWithTargets
        startGameLoop()
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
        }
    }

    fun onAction(action: GameAction) {
        _state.update { GameEngine.processAction(it, action) }
    }

    fun resetGame() {
        gameLoopJob?.cancel()
        _state.update { GameState.initial() }
    }

    fun onCleared() {
        gameLoopJob?.cancel()
    }
}