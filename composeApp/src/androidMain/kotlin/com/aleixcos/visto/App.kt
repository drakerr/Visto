package com.aleixcos.visto

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aleixcos.visto.domain.BoardItem
import com.aleixcos.visto.domain.GamePhase
import com.aleixcos.visto.domain.GameState
import com.aleixcos.visto.engine.GameAction
import com.aleixcos.visto.presentation.GameViewModel

private val COLS = 5
private val ROWS = 8

@Composable
fun App() {
    val viewModel = remember { GameViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    MaterialTheme {
        when (state.phase) {
            GamePhase.COUNTDOWN, GamePhase.FINISHED -> MenuScreen(
                state = state,
                onStartGame = { viewModel.startGame() }
            )
            GamePhase.PLAYING -> GameScreen(
                state = state,
                onItemTap = { viewModel.onAction(GameAction.TapItem(it)) }
            )
        }
    }
}

// MARK: - Menu

@Composable
fun MenuScreen(state: GameState, onStartGame: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("🔍 VISTO", fontSize = 52.sp, fontWeight = FontWeight.Black,
                color = Color.White, modifier = Modifier.scale(pulse))
            Text(
                "Encuentra los objetos\nantes que tu rival",
                fontSize = 16.sp, color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center
            )
            if (state.foundCount > 0) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF))
                ) {
                    Column(
                        Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${state.score} pts", fontSize = 28.sp,
                            fontWeight = FontWeight.Black, color = Color.White)
                        Text("${state.foundCount} encontrados",
                            fontSize = 14.sp, color = Color(0xFFAAAAAA))
                    }
                }
            }
            Button(
                onClick = onStartGame,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(52.dp).width(200.dp)
            ) {
                Text(
                    text = if (state.foundCount > 0) "Jugar de nuevo" else "¡Jugar!",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// MARK: - Game

@Composable
fun GameScreen(state: GameState, onItemTap: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFFFBF0))) {

        // HUD
        HUDView(state = state)

        // Tablero — grid fijo 5x8
        BoardGrid(
            state = state,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onItemTap = onItemTap
        )

        // Barra objetivos
        TargetsBar(targets = state.activeTargets, foundCount = state.foundCount)
    }
}

// MARK: - HUD

@Composable
fun HUDView(state: GameState) {
    val timerColor = when {
        state.timeRemainingMs < 10_000L -> Color(0xFFE53935)
        state.timeRemainingMs < 20_000L -> Color(0xFFFB8C00)
        else -> Color(0xFF43A047)
    }
    val timeRatio = (state.timeRemainingMs / 60_000f).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = timeRatio, animationSpec = tween(100), label = "timer"
    )
    val comboScale by animateFloatAsState(
        targetValue = if (state.combo >= 2) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "combo"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⏱ ${state.timeRemainingMs / 1000}s",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = timerColor)
            Text("⭐ ${state.score}",
                fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("🔥 x${state.combo}",
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = if (state.combo >= 3) Color(0xFFFF6B00) else Color.Gray,
                modifier = Modifier.scale(comboScale))
        }
        LinearProgressIndicator(
            progress = { animatedRatio },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
            color = timerColor, trackColor = Color(0xFFE0E0E0)
        )
    }
}

// MARK: - Board Grid

@Composable
fun BoardGrid(
    state: GameState,
    modifier: Modifier = Modifier,
    onItemTap: (Int) -> Unit
) {
    // Grid fijo 5 columnas x 8 filas — todos los items visibles
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val rows = state.board.items.chunked(COLS)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowItems.forEach { item ->
                    val isTarget = state.activeTargets.any { it.id == item.id }
                    BoardItemView(
                        item = item,
                        isTarget = isTarget,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onTap = { onItemTap(item.id) }
                    )
                }
                // Rellenar si la fila está incompleta
                repeat(COLS - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// MARK: - Board Item

@Composable
fun BoardItemView(
    item: BoardItem,
    isTarget: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow_${item.id}")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "glow_a_${item.id}"
    )

    val scale by animateFloatAsState(
        targetValue = if (isTarget) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "scale_${item.id}"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isTarget) Color(0xFFFFD700).copy(alpha = 0.15f)
                else Color(0xFFF5F0E8)
            )
            .then(
                if (isTarget) Modifier.border(
                    2.dp,
                    Color(0xFFFFD700).copy(alpha = glowAlpha),
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = item.imageKey, fontSize = 28.sp)
    }
}

// MARK: - Targets Bar

@Composable
fun TargetsBar(targets: List<BoardItem>, foundCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🎯 BUSCA:", fontSize = 12.sp,
                fontWeight = FontWeight.Bold, color = Color(0xFFAAAAAA),
                letterSpacing = 1.sp)
            Text("✅ $foundCount encontrados", fontSize = 12.sp,
                color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            targets.forEach { target ->
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F3460))
                        .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(target.imageKey, fontSize = 32.sp)
                }
            }
        }
    }
}