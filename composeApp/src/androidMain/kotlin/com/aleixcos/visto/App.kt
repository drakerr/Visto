package com.aleixcos.visto

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aleixcos.visto.domain.BoardItem
import com.aleixcos.visto.domain.GamePhase
import com.aleixcos.visto.domain.GameState
import com.aleixcos.visto.engine.GameAction
import com.aleixcos.visto.presentation.GameViewModel
import kotlinx.coroutines.delay
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.alpha

private const val COLS = 5

@Composable
fun App() {
    val viewModel = remember { GameViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isTransitioning by remember { mutableStateOf(false) }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.phase,
                transitionSpec = {
                    if (targetState == GamePhase.PLAYING) {
                        slideInVertically { it } + fadeIn() togetherWith fadeOut()
                    } else {
                        fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                    }
                },
                label = "phase"
            ) { phase ->
                when (phase) {
                    GamePhase.COUNTDOWN, GamePhase.FINISHED -> MenuScreen(
                        state = state,
                        onStartGame = {
                            isTransitioning = true
                            viewModel.startGame()
                        }
                    )
                    GamePhase.PLAYING -> GameScreen(
                        state = state,
                        onItemTap = { viewModel.onAction(GameAction.TapItem(it)) },
                        onUsePowerUp = { viewModel.onAction(GameAction.UsePowerUp(it)) }
                    )
                }
            }

            // Flash de transición
            AnimatedVisibility(
                visible = isTransitioning,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
                LaunchedEffect(Unit) {
                    delay(300)
                    isTransitioning = false
                }
            }
        }
    }
}

// MARK: - Menu

@Composable
fun MenuScreen(state: GameState, onStartGame: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "float"
    )

    val bgEmojis = listOf(
        "🐶","🐱","🦊","🐸","🦋","🐝","🦁","🐼","🦄","🐙",
        "🦀","🐬","🦅","🌺","⭐","🔥","🌈","🎯","🏆","💎"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E), Color(0xFF0F3460))
                )
            )
    ) {
        // Emojis decorativos de fondo
        bgEmojis.forEachIndexed { index, emoji ->
            val x = ((index * 137) % 360).dp
            val y = ((index * 89) % 700).dp
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier
                    .offset(x = x, y = y)
                    .alpha(0.08f)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(80.dp))

            // Logo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🔍",
                    fontSize = 72.sp,
                    modifier = Modifier.offset(y = floatOffset.dp)
                )
                Text(
                    text = "VISTO",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 8.sp
                )
                Text(
                    text = "Encuentra · Compite · Gana",
                    fontSize = 13.sp,
                    color = Color(0xFFFFD700),
                    letterSpacing = 2.sp
                )
            }

            // Stats partida previa
            if (state.foundCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    StatBadge("${state.score}", "puntos", "⭐", Modifier.weight(1f))
                    StatBadge("${state.foundCount}", "hallados", "🎯", Modifier.weight(1f))
                }
            }

            // Botón jugar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                Button(
                    onClick = onStartGame,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .scale(pulse)
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = "¡JUGAR!  ▶",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A1A2E),
                        letterSpacing = 2.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Combo x3 · x5 · x7 para ganar power-ups",
                    fontSize = 11.sp,
                    color = Color(0xFF666688),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StatBadge(value: String, label: String, emoji: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(label, fontSize = 10.sp, color = Color(0xFFAAAAAA), letterSpacing = 1.sp)
    }
}

// MARK: - Game

@Composable
fun GameScreen(
    state: GameState,
    onItemTap: (Int) -> Unit,
    onUsePowerUp: (String) -> Unit
) {
    val view = LocalView.current
    var lastFoundId by remember { mutableStateOf<Int?>(null) }
    var showWrongFlash by remember { mutableStateOf(false) }
    var prevFoundCount by remember { mutableIntStateOf(state.foundCount) }
    var prevWrongCount by remember { mutableIntStateOf(state.wrongTapCount) }
    var prevCombo by remember { mutableIntStateOf(state.combo) }

    LaunchedEffect(state.foundCount) {
        if (state.foundCount > prevFoundCount) {
            prevFoundCount = state.foundCount
            ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.CONFIRM)
            delay(350)
            lastFoundId = null
        }
    }

    LaunchedEffect(state.wrongTapCount) {
        if (state.wrongTapCount > prevWrongCount) {
            prevWrongCount = state.wrongTapCount
            ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.REJECT)
            showWrongFlash = true
            delay(300)
            showWrongFlash = false
        }
    }

    LaunchedEffect(state.combo) {
        if (state.combo >= 3 && state.combo > prevCombo) {
            ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.LONG_PRESS)
        }
        prevCombo = state.combo
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showWrongFlash) Color(0x22FF0000) else Color(0xFFFFFBF0))
    ) {
        HUDView(state = state)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            BoardGrid(
                state = state,
                lastFoundId = lastFoundId,
                modifier = Modifier.fillMaxSize(),
                onItemTap = { itemId ->
                    val isTarget = state.activeTargets.any { it.id == itemId }
                    if (isTarget) lastFoundId = itemId
                    onItemTap(itemId)
                }
            )
            // Combo overlay
            ComboOverlay(
                combo = state.combo,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
            )
        }
        PowerUpsBar(state = state, onUsePowerUp = onUsePowerUp)
        TargetsBar(targets = state.activeTargets, foundCount = state.foundCount)
    }
}

// MARK: - Combo Overlay

@Composable
fun ComboOverlay(combo: Int, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var displayCombo by remember { mutableIntStateOf(combo) }

    val comboColor = when {
        combo >= 10 -> Color(0xFFFFD700)
        combo >= 7  -> Color(0xFFFF9800)
        combo >= 5  -> Color(0xFF2196F3)
        else        -> Color(0xFF4CAF50)
    }

    val comboText = when {
        combo >= 10 -> "⭐ ¡Combo x$combo!"
        combo >= 7  -> "🔍 ¡Combo x$combo!"
        combo >= 5  -> "❄️ ¡Combo x$combo!"
        else        -> "🛡️ ¡Combo x$combo!"
    }

    LaunchedEffect(combo) {
        if (combo >= 3) {
            displayCombo = combo
            visible = true
            delay(1200)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -40 } + fadeIn(tween(200)),
        exit = slideOutVertically { -40 } + fadeOut(tween(300)),
        modifier = modifier
    ) {
        Text(
            text = comboText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(comboColor)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .shadow(8.dp, RoundedCornerShape(50.dp))
        )
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
    val isWinning = state.score >= state.ghostSnapshot.currentScore
    val scoreDiff = state.score - state.ghostSnapshot.currentScore

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isWinning) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(if (isWinning) "👑" else "💀", fontSize = 14.sp)
                Text("Rival", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (isWinning) Color(0xFF2E7D32) else Color(0xFFC62828))
                Text("· ${state.ghostSnapshot.foundCount} obj",
                    fontSize = 12.sp, color = Color.Gray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("⭐ ${state.ghostSnapshot.currentScore}",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (isWinning) Color(0xFF2E7D32) else Color(0xFFC62828))
                val diffText = if (scoreDiff >= 0) "+$scoreDiff" else "$scoreDiff"
                Text("($diffText)", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (isWinning) Color(0xFF43A047) else Color(0xFFE53935))
            }
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
    lastFoundId: Int?,
    modifier: Modifier = Modifier,
    onItemTap: (Int) -> Unit
) {
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
                    BoardItemView(
                        item = item,
                        isJustFound = lastFoundId == item.id,
                        isRevealed = state.revealedItemId == item.id,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onTap = { onItemTap(item.id) }
                    )
                }
                repeat(COLS - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

// MARK: - Board Item

@Composable
fun BoardItemView(
    item: BoardItem,
    isJustFound: Boolean,
    isRevealed: Boolean = false,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val revealPulse = rememberInfiniteTransition(label = "reveal_${item.id}")
    val revealAlpha by revealPulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "ra_${item.id}"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isJustFound -> Color(0xFF4CAF50)
            isRevealed  -> Color(0xFFFFD700)
            else        -> Color(0xFFF5F0E8)
        },
        animationSpec = tween(200),
        label = "bg_${item.id}"
    )
    val emojiScale by animateFloatAsState(
        targetValue = if (isJustFound) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "scale_${item.id}"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor.copy(alpha = if (isRevealed) revealAlpha else 1f))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = item.imageKey, fontSize = 28.sp,
            modifier = Modifier.scale(emojiScale))
    }
}

// MARK: - Power-Ups Bar

@Composable
fun PowerUpsBar(state: GameState, onUsePowerUp: (String) -> Unit) {
    val allPowerUps = listOf(
        Triple("reveal", "🔍", "Revelar"),
        Triple("double_points", "⭐", "x2"),
        Triple("freeze_time", "❄️", "Congelar"),
        Triple("shuffle", "🔀", "Mezclar"),
        Triple("combo_shield", "🛡️", "Escudo")
    )
    val available = allPowerUps.filter { (id, _, _) -> state.chargesFor(id) > 0 }
    if (available.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        available.forEach { (id, emoji, label) ->
            val charges = state.chargesFor(id)
            val isActive = state.activePowerUps.any { it.powerUpId == id } ||
                    (id == "combo_shield" && state.hasComboShield)
            PowerUpButton(emoji, label, charges, isActive) { onUsePowerUp(id) }
        }
    }
}

@Composable
fun PowerUpButton(
    emoji: String,
    label: String,
    charges: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "pu_scale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) Color(0xFF1A1A2E) else Color(0xFF2A2A3E))
            .then(
                if (isActive) Modifier.border(2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(label, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(charges) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFFFD700)))
            }
        }
    }
}

// MARK: - Targets Bar

@Composable
fun TargetsBar(targets: List<BoardItem>, foundCount: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Separador dorado
        Box(modifier = Modifier.fillMaxWidth().height(1.dp)
            .background(Color(0xFFFFD700).copy(alpha = 0.3f)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contador
            Column(
                modifier = Modifier.width(64.dp).padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "$foundCount",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700)
                )
                Text(
                    text = "hallados",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA),
                    letterSpacing = 0.5.sp
                )
            }

            // Separador vertical
            Box(modifier = Modifier.width(1.dp).height(50.dp)
                .background(Color.White.copy(alpha = 0.1f)))

            // Objetivos
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                targets.forEach { target ->
                    TargetCard(item = target)
                }
            }
        }
    }
}

@Composable
fun TargetCard(item: BoardItem) {
    var appeared by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "target_appear"
    )
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(200),
        label = "target_alpha"
    )

    LaunchedEffect(Unit) { appeared = true }

    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F3460))
            .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(item.imageKey, fontSize = 28.sp)
    }
}