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
import androidx.compose.ui.draw.blur
import com.aleixcos.visto.domain.GameResult
import kotlinx.coroutines.launch

private const val COLS = 5

@Composable
fun App() {
    val viewModel = remember { GameViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isTransitioning by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                    GamePhase.IDLE -> MenuScreen(
                        state = state,
                        viewModel = viewModel,
                        context = context,
                        onStartGame = {
                            isTransitioning = true
                            viewModel.startGame()
                        }
                    )
                    GamePhase.COUNTDOWN -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Tablero con blur detrás
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .blur(8.dp)
                            ) {
                                GameScreen(
                                    state = state,
                                    onItemTap = { },
                                    onUsePowerUp = { }
                                )
                            }
                            // Overlay encima
                            CountdownOverlay(state = state)
                        }
                    }
                    GamePhase.PLAYING -> GameScreen(
                        state = state,
                        onItemTap = { viewModel.onAction(GameAction.TapItem(it)) },
                        onUsePowerUp = { viewModel.onAction(GameAction.UsePowerUp(it)) }
                    )
                    GamePhase.FINISHED -> {
                        state.result?.let { result ->
                            ResultScreen(
                                result = result,
                                onPlayAgain = {
                                    isTransitioning = true
                                    viewModel.resetGame()
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isTransitioning,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200))
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
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
fun MenuScreen(
    state: GameState,
    viewModel: GameViewModel,
    context: android.content.Context,
    onStartGame: () -> Unit
) {
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

    var isSigningIn by remember { mutableStateOf(false) }
    var isAnonymous by remember { mutableStateOf(viewModel.isAnonymous()) }
    var isLoggedIn by remember { mutableStateOf(viewModel.isLoggedIn()) }
    var username by remember { mutableStateOf(viewModel.currentUsername()) }
    var avatar by remember { mutableStateOf(viewModel.currentAvatar()) }
    val coroutineScope = rememberCoroutineScope()

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
        // Emojis decorativos
        bgEmojis.forEachIndexed { index, emoji ->
            Text(
                text = emoji, fontSize = 28.sp,
                modifier = Modifier
                    .offset(x = ((index * 137) % 360).dp, y = ((index * 89) % 700).dp)
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
                Text(text = "🔍", fontSize = 72.sp,
                    modifier = Modifier.offset(y = floatOffset.dp))
                Text("VISTO", fontSize = 52.sp, fontWeight = FontWeight.Black,
                    color = Color.White, letterSpacing = 8.sp)
                Text("Encuentra · Compite · Gana", fontSize = 13.sp,
                    color = Color(0xFFFFD700), letterSpacing = 2.sp)
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

            // Botones
            Column(
                modifier = Modifier.padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isAnonymous || !isLoggedIn) {
                    // Google Sign-In
                    Button(
                        onClick = {
                            isSigningIn = true
                            coroutineScope.launch {
                                try {
                                    val idToken = GoogleSignInHelper.signIn(context)
                                    viewModel.signInWithGoogle(idToken) { success, error ->
                                        if (success) {
                                            isAnonymous = viewModel.isAnonymous()
                                            isLoggedIn = viewModel.isLoggedIn()
                                            username = viewModel.currentUsername()
                                            avatar = viewModel.currentAvatar()
                                        } else {
                                            println("❌ Google Sign-In error: $error")
                                        }
                                        isSigningIn = false
                                    }
                                } catch (e: Exception) {
                                    println("❌ Google Sign-In error: ${e.message}")
                                    isSigningIn = false
                                }
                            }
                        },
                        enabled = !isSigningIn,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF4285F4),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("G", fontSize = 20.sp, fontWeight = FontWeight.Black,
                                    color = Color(0xFF4285F4))
                                Text("Continuar con Google", fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
                            }
                        }
                    }

                    // Separador
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.15f)))
                        Text("o", fontSize = 12.sp, color = Color(0xFF666688),
                            modifier = Modifier.padding(horizontal = 12.dp))
                        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.15f)))
                    }

                    // Jugar como invitado
                    Button(
                        onClick = onStartGame,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    ) {
                        Text("Jugar como invitado", fontSize = 16.sp,
                            color = Color(0xFFAAAAAA), fontWeight = FontWeight.Medium)
                    }

                } else {
                    // Usuario registrado
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(avatar, fontSize = 24.sp)
                        Text(username, fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    // Botón jugar
                    Button(
                        onClick = onStartGame,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(58.dp).scale(pulse)
                    ) {
                        Text("¡JUGAR!  ▶", fontSize = 20.sp,
                            fontWeight = FontWeight.Black, color = Color(0xFF1A1A2E),
                            letterSpacing = 2.sp)
                    }
                }

                Text(
                    "Combo x3 · x5 · x7 para ganar power-ups",
                    fontSize = 11.sp, color = Color(0xFF666688),
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

// MARK: - Result Screen

@Composable
fun ResultScreen(
    result: GameResult,
    onPlayAgain: () -> Unit
) {
    val view = LocalView.current
    var showOutcome by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showBreakdown by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }

    val outcomeEmoji = when (result.outcome) {
        GameResult.Outcome.WIN  -> "🏆"
        GameResult.Outcome.LOSE -> "💀"
        else                    -> "🤝"
    }
    val outcomeText = when (result.outcome) {
        GameResult.Outcome.WIN  -> "¡VICTORIA!"
        GameResult.Outcome.LOSE -> "DERROTA"
        else                    -> "EMPATE"
    }
    val outcomeColor = when (result.outcome) {
        GameResult.Outcome.WIN  -> Color(0xFFFFD700)
        GameResult.Outcome.LOSE -> Color(0xFFE53935)
        else                    -> Color(0xFF78909C)
    }
    val bgColors = when (result.outcome) {
        GameResult.Outcome.WIN  -> listOf(Color(0xFF0D1B00), Color(0xFF1A3300), Color(0xFF0F3460))
        GameResult.Outcome.LOSE -> listOf(Color(0xFF1A0000), Color(0xFF2D0000), Color(0xFF0F3460))
        else                    -> listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E), Color(0xFF0F3460))
    }

    LaunchedEffect(Unit) {
        delay(100); showOutcome = true
        delay(300); showStats = true
        delay(300); showBreakdown = true
        delay(300); showButtons = true
        when (result.outcome) {
            GameResult.Outcome.WIN  -> ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.LONG_PRESS)
            GameResult.Outcome.LOSE -> ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.REJECT)
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgColors))
    ) {
        // Partículas victoria
        if (result.outcome == GameResult.Outcome.WIN) {
            ParticlesView()
        }

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // Outcome
            item {
                AnimatedVisibility(
                    visible = showOutcome,
                    enter = scaleIn() + fadeIn()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(outcomeEmoji, fontSize = 80.sp)
                        Text(
                            text = outcomeText,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = outcomeColor,
                            letterSpacing = 4.sp
                        )
                    }
                }
            }

            // Rival card
            item {
                AnimatedVisibility(
                    visible = showStats,
                    enter = slideInHorizontally { -it } + fadeIn()
                ) {
                    RivalCard(result = result)
                }
            }

            // Score comparison
            item {
                AnimatedVisibility(
                    visible = showStats,
                    enter = slideInHorizontally { it } + fadeIn()
                ) {
                    ScoreComparisonCard(result = result, outcomeColor = outcomeColor)
                }
            }

            // Breakdown
            item {
                AnimatedVisibility(
                    visible = showBreakdown,
                    enter = slideInVertically { it } + fadeIn()
                ) {
                    BreakdownCard(result = result)
                }
            }

            // Botón
            item {
                AnimatedVisibility(
                    visible = showButtons,
                    enter = slideInVertically { it } + fadeIn()
                ) {
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = outcomeColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Text(
                            "¡JUGAR DE NUEVO!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A1A2E),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Rival Card

@Composable
fun RivalCard(result: GameResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F3460)),
            contentAlignment = Alignment.Center
        ) {
            Text(result.rivalAvatar, fontSize = 30.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(result.rivalUsername,
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Ranking #${result.rivalRank}",
                fontSize = 12.sp, color = Color(0xFFAAAAAA))
        }
    }
}

// MARK: - Score Comparison

@Composable
fun ScoreComparisonCard(result: GameResult, outcomeColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("RESULTADO", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFFAAAAAA), letterSpacing = 2.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TÚ", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA), letterSpacing = 1.sp)
                Text("${result.localScore}", fontSize = 36.sp,
                    fontWeight = FontWeight.Black, color = Color.White)
                Text("${result.localFound} obj", fontSize = 13.sp,
                    color = Color(0xFF4CAF50))
            }
            Text("VS", fontSize = 18.sp, fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RIVAL", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA), letterSpacing = 1.sp)
                Text("${result.ghostScore}", fontSize = 36.sp,
                    fontWeight = FontWeight.Black, color = Color.White)
                Text("${result.ghostFound} obj", fontSize = 13.sp,
                    color = Color(0xFFE53935))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔥 Mejor racha:", fontSize = 13.sp, color = Color(0xFFAAAAAA))
            Text("x${result.maxCombo}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (result.maxCombo >= 5) Color(0xFFFF6B00) else Color.White)
        }
    }
}

// MARK: - Breakdown Card

@Composable
fun BreakdownCard(result: GameResult) {
    val localTotal = result.localFound + result.wrongTapCount
    val localAccuracy = if (localTotal > 0) (result.localFound * 100 / localTotal) else 0
    val rivalTotal = result.ghostFound + result.ghostWrongTaps
    val rivalAccuracy = if (rivalTotal > 0) (result.ghostFound * 100 / rivalTotal) else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("ESTADÍSTICAS", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFFAAAAAA), letterSpacing = 2.sp)

        // Cabecera columnas
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text("TÚ", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50), modifier = Modifier.width(64.dp),
                textAlign = TextAlign.End)
            Text("RIVAL", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFFE53935), modifier = Modifier.width(64.dp),
                textAlign = TextAlign.End)
        }

        ResultDivider()
        ResultRow("🎯", "Objetos hallados", "${result.localFound}", "${result.ghostFound}")
        ResultDivider()
        ResultRow("⭐", "Puntos acumulados", "${result.basePoints}", "${result.ghostScore}")
        ResultDivider()
        ResultRow("🔥", "Mejor racha", "x${result.maxCombo}", "x${result.ghostMaxCombo}")
        ResultDivider()

        // Precisión
        ResultRow("🎯", "Precisión", "$localAccuracy%", "$rivalAccuracy%")
        ResultSubRow("↳ Aciertos", "${result.localFound}", "${result.ghostFound}",
            Color(0xFF4CAF50), Color(0xFFE53935))
        ResultSubRow("↳ Fallos", "${result.wrongTapCount}", "${result.ghostWrongTaps}",
            Color(0xFFE53935), Color(0xFF666688))

        ResultDivider(alpha = 0.2f)

        // Total
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("PUNTUACIÓN", fontSize = 13.sp, fontWeight = FontWeight.Black,
                color = Color.White, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Text("${result.localScore}", fontSize = 15.sp, fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700), modifier = Modifier.width(64.dp),
                textAlign = TextAlign.End)
            Text("${result.ghostScore}", fontSize = 15.sp, fontWeight = FontWeight.Black,
                color = Color(0xFFAAAAAA), modifier = Modifier.width(64.dp),
                textAlign = TextAlign.End)
        }
    }
}

@Composable
fun ResultRow(emoji: String, label: String, localValue: String, rivalValue: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 13.sp)
        Text(" $label", fontSize = 12.sp, color = Color(0xFFAAAAAA),
            modifier = Modifier.weight(1f))
        Text(localValue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = Color.White, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
        Text(rivalValue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFF666688), modifier = Modifier.width(64.dp),
            textAlign = TextAlign.End)
    }
}

@Composable
fun ResultSubRow(
    label: String,
    localValue: String,
    rivalValue: String,
    localColor: Color,
    rivalColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("     $label", fontSize = 11.sp, color = Color(0xFF666688),
            modifier = Modifier.weight(1f))
        Text(localValue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = localColor, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
        Text(rivalValue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = rivalColor, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
    }
}

@Composable
fun ResultDivider(alpha: Float = 0.1f) {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp)
        .background(Color.White.copy(alpha = alpha)))
}

// MARK: - Particles

@Composable
fun ParticlesView() {
    val emojis = listOf("⭐","🏆","✨","🎉","🎊","💫","🌟")
    val particles = remember {
        (0..19).map { i ->
            Triple(i, (i * 137 % 360).toFloat(), emojis[i % emojis.size])
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { (index, x, emoji) ->
            var offsetY by remember { mutableFloatStateOf(-40f) }
            var alpha by remember { mutableFloatStateOf(1f) }

            LaunchedEffect(Unit) {
                delay(index * 100L)
                val duration = (1500..3000).random().toLong()
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                    offsetY = -40f + progress * 900f
                    alpha = 1f - progress
                    if (progress >= 1f) break
                    delay(16)
                }
            }
            Text(
                text = emoji,
                fontSize = 20.sp,
                modifier = Modifier
                    .offset(x = x.dp, y = offsetY.dp)
                    .alpha(alpha)
            )
        }
    }
}
@Composable
fun CountdownOverlay(state: GameState) {
    var count by remember { mutableIntStateOf(3) }
    var scale by remember { mutableFloatStateOf(1.4f) }
    var showTargets by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "countdown_scale"
    )

    LaunchedEffect(Unit) {
        showTargets = true
        repeat(3) { i ->
            if (i > 0) {
                count = 3 - i
                scale = 1.4f
                delay(50)
                scale = 1f
            } else {
                scale = 1f
            }
            delay(1_000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Spacer(Modifier.height(32.dp))

            // Título
            AnimatedVisibility(
                visible = showTargets,
                enter = fadeIn(tween(300))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "¡PREPÁRATE!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        letterSpacing = 4.sp
                    )
                    Text(
                        "Encuentra estos objetos",
                        fontSize = 15.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }

            // Objetivos
            AnimatedVisibility(
                visible = showTargets,
                enter = scaleIn() + fadeIn()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    state.activeTargets.forEach { target ->
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F3460))
                                .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(target.imageKey, fontSize = 36.sp)
                        }
                    }
                }
            }

            // Cuenta atrás
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { count / 3f },
                    modifier = Modifier.size(100.dp),
                    color = Color(0xFFFFD700),
                    trackColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                    strokeWidth = 4.dp
                )
                Text(
                    text = "$count",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.scale(animatedScale)
                )
            }

            Text(
                "¡A buscar!",
                fontSize = 14.sp,
                color = Color(0xFF666688)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}