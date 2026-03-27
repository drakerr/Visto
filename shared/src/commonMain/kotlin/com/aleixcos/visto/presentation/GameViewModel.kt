package com.aleixcos.visto.presentation

import com.aleixcos.visto.domain.GamePhase
import com.aleixcos.visto.domain.GameState
import com.aleixcos.visto.domain.GhostRun
import com.aleixcos.visto.engine.BoardGenerator
import com.aleixcos.visto.engine.GameAction
import com.aleixcos.visto.engine.GameEngine
import com.aleixcos.visto.ghostrun.GhostRunMock
import com.aleixcos.visto.ghostrun.GhostRunRecorder
import com.aleixcos.visto.network.AuthRepository
import com.aleixcos.visto.network.RunRepository
import com.aleixcos.visto.network.SupabaseAuthRepository
import com.aleixcos.visto.network.SupabaseRunRepository
import com.aleixcos.visto.network.createHttpClient
import com.aleixcos.visto.network.createSupabaseClient
import com.aleixcos.visto.network.dto.PlayerDto
import io.github.jan.supabase.auth.auth
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
    private var lastRecordedRun: GhostRun? = null
    val recordedRun: GhostRun? get() = lastRecordedRun
    private val _state = MutableStateFlow(GameState.initial())
    val state: StateFlow<GameState> = _state.asStateFlow()
    private val httpClient = createHttpClient()
    private val supabase = createSupabaseClient()
    private val authRepository: AuthRepository = SupabaseAuthRepository(supabase)
    private val repository: RunRepository = SupabaseRunRepository(httpClient)
    private var currentPlayer: PlayerDto? = null

    init {
        scope.launch {
            initPlayer()
        }
    }

    private suspend fun initPlayer() {
        if (authRepository.isLoggedIn()) {
            currentPlayer = authRepository.getCurrentPlayer()
        } else {
            authRepository.signInAnonymously()
                .onSuccess { currentPlayer = it }
                .onFailure { println("❌ Error en autenticación: ${it.message}") }
        }
        println("👤 Jugador: ${currentPlayer?.username} (${currentPlayer?.id})")
    }

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
        val playerId = currentPlayer?.id ?: return

        lastRecordedRun = recorder.buildRun(
            runId = "run_${state.seed}_${Clock.System.now().toEpochMilliseconds()}",
            playerId = playerId,
            seed = state.seed,
            finalScore = state.score,
            foundCount = state.foundCount,
            durationMs = 60_000L - state.timeRemainingMs
        )

        // Subir en background — el jugador no sabe que pasa
        lastRecordedRun?.let { run ->
            scope.launch {
                println("🔵 Intentando subir run: ${run.runId}")
                repository.uploadRun(run)
                    .onSuccess { println("✅ Run subido correctamente") }
                    .onFailure { println("❌ Error subiendo run: ${it.message}") }
            }
        }    }

    fun resetGame() {
        gameLoopJob?.cancel()
        _state.update { GameState.initial() }
    }

    fun onCleared() {
        gameLoopJob?.cancel()
    }

    fun signInWithGoogle(idToken: String, callback: (Boolean, String?) -> Unit) {
        scope.launch {
            authRepository.signInWithGoogle(idToken)
                .onSuccess {
                    currentPlayer = it
                    callback(true, null)
                }
                .onFailure {
                    callback(false, it.message)
                }
        }
    }

    fun signInWithApple(idToken: String, callback: (Boolean, String?) -> Unit) {
        scope.launch {
            authRepository.signInWithApple(idToken)
                .onSuccess {
                    currentPlayer = it
                    callback(true, null)
                }
                .onFailure {
                    callback(false, it.message)
                }
        }
    }

    fun isLoggedIn(): Boolean = currentPlayer != null
    fun isAnonymous(): Boolean {
        val user = supabase.auth.currentUserOrNull() ?: return true
        return user.appMetadata?.get("provider")?.toString()?.contains("anonymous") == true
    }
    fun currentUsername(): String = currentPlayer?.username ?: "Invitado"
    fun currentAvatar(): String = currentPlayer?.avatar ?: "👤"
}