package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.*
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.abs

// Design Space Theme Colors
val CyberDarkBg = Color(0xFF07090E)        // OLED pure canvas backing
val CyberCardBg = Color(0xFF11141E)        // Tech terminal surface card
val CyberCardBorder = Color(0xFF22283A)    // Cyber borders
val NeonGreen = Color(0xFF39FF14)          // Emerald synth laser green
val NeonCherry = Color(0xFFFF2A6D)         // Synthetic neon red/cherry
val NeonGold = Color(0xFFFFB200)           // Rare golden bonus food
val CyberSlate = Color(0xFF1E2436)         // D-pad gray elements
val NeonYellowGlow = Color(0xFFFFEA00)     // Classic scoreboard gold yellow
val TransparentOverlay = Color(0xCC05060A) // Background fade during modal pauses

class MainActivity : ComponentActivity() {
    
    // Use lazy delegation for the ViewModel so it matches safe runtime lifecycles
    private val gameEngine: SnakeGameEngine by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = CyberDarkBg
                ) { innerPadding ->
                    SnakeGameApp(
                        engine = gameEngine,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Automatically pause running gameplay when application leaves foreground
        gameEngine.pauseGame()
    }
}

// Visual layout root matching navigation states
@Composable
fun SnakeGameApp(
    engine: SnakeGameEngine,
    modifier: Modifier = Modifier
) {
    val uiState by engine.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
    ) {
        when (uiState.gameState) {
            GameState.MENU -> {
                MenuScreen(
                    currentDifficulty = uiState.difficulty,
                    highHighScore = uiState.highHighScore,
                    isMuted = uiState.isSoundMuted,
                    onSelectDifficulty = { engine.setDifficulty(it) },
                    onStartGame = { engine.startGame() },
                    onToggleMute = { engine.toggleMute() }
                )
            }
            GameState.RUNNING, GameState.PAUSED, GameState.GAME_OVER -> {
                GameplayScreen(
                    state = uiState,
                    onDirectionInput = { engine.handleDirectionInput(it) },
                    onPauseGame = { engine.pauseGame() },
                    onResumeGame = { engine.resumeGame() },
                    onRestartGame = { engine.startGame() },
                    onExitToMenu = { engine.exitToMenu() },
                    onToggleMute = { engine.toggleMute() }
                )
            }
        }
    }
}

// 1. Classic Arcade Retro Home Setup
@Composable
fun MenuScreen(
    currentDifficulty: Difficulty,
    highHighScore: Int,
    isMuted: Boolean,
    onSelectDifficulty: (Difficulty) -> Unit,
    onStartGame: () -> Unit,
    onToggleMute: () -> Unit
) {
    // Dynamic animation on title letters to establish the nostalgic cyberpunk vibe
    val infiniteTransition = rememberInfiniteTransition(label = "title_glow")
    val titleGlowOffset by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Sound Configuration - Text-based, classic chiptune style for safety & retro weight
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onToggleMute,
                modifier = Modifier
                    .height(42.dp)
                    .testTag("mute_toggle_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCardBg,
                    contentColor = if (isMuted) Color.Gray else NeonGreen
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isMuted) "SOUND [ OFF ]" else "SOUND [ ON ]",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Title and Arcade Brand Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "SNAKE",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 52.sp,
                    color = Color.White,
                    shadow = Shadow(
                        color = NeonGreen,
                        offset = Offset(0f, 0f),
                        blurRadius = titleGlowOffset
                    )
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "RETRO",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    letterSpacing = 6.sp,
                    color = NeonCherry,
                    shadow = Shadow(
                        color = NeonCherry,
                        offset = Offset(0f, 0f),
                        blurRadius = titleGlowOffset / 2
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Scoreboard display
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
                    .background(CyberCardBg)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Star icon",
                        tint = NeonYellowGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.high_score_label, highHighScore),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("high_score_text")
                    )
                }
            }
        }

        // Difficulty selectors & Control instructions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.difficulty_label),
                color = Color.LightGray,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )

            // Dynamic Radio Cards for choosing Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Difficulty.values().forEach { diff ->
                    val isSelected = currentDifficulty == diff
                    val buttonColor = if (isSelected) CyberSlate else CyberCardBg
                    val borderColor = if (isSelected) NeonGreen else CyberCardBorder
                    val textColor = if (isSelected) NeonGreen else Color.Gray
                    val tagSuffix = diff.name.lowercase()

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(buttonColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable { onSelectDifficulty(diff) }
                            .padding(vertical = 12.dp)
                            .testTag("difficulty_${tagSuffix}_button")
                    ) {
                        Text(
                            text = when (diff) {
                                Difficulty.EASY -> stringResource(R.string.difficulty_easy)
                                Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
                                Difficulty.HARD -> stringResource(R.string.difficulty_hard)
                            },
                            color = textColor,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Play Trigger Option
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("play_game_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_play),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User tutorial
            Text(
                text = stringResource(R.string.instructions),
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// 2. Main Gameplay Setup (with custom canvas render and D-pad)
@Composable
fun GameplayScreen(
    state: SnakeUiState,
    onDirectionInput: (Direction) -> Unit,
    onPauseGame: () -> Unit,
    onResumeGame: () -> Unit,
    onRestartGame: () -> Unit,
    onExitToMenu: () -> Unit,
    onToggleMute: () -> Unit
) {
    val density = LocalDensity.current
    var accumulatedDragX by remember { mutableStateOf(0f) }
    var accumulatedDragY by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Stats Score Row at the Top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.score_label, state.score),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("score_text")
                )
                Text(
                    text = stringResource(R.string.high_score_label, state.highHighScore),
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Toggle Link using standard chiptune-styled text badge button
                Button(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("mute_toggle_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCardBg,
                        contentColor = if (state.isSoundMuted) Color.Gray else NeonGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (state.isSoundMuted) "MUTED" else "SOUND",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Pause toggle button using standard core playback icons
                IconButton(
                    onClick = {
                        if (state.gameState == GameState.RUNNING) {
                            onPauseGame()
                        } else if (state.gameState == GameState.PAUSED) {
                            onResumeGame()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberCardBg)
                        .border(1.dp, CyberCardBorder, CircleShape)
                        .testTag("pause_toggle_button")
                ) {
                    Icon(
                        imageVector = if (state.gameState == GameState.PAUSED) Icons.Filled.PlayArrow else Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.btn_resume),
                        tint = NeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Active Game Board - Bound to AspectRatio so it displays perfectly on all screens
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .aspectRatio(SnakeGameEngine.GRID_COLUMNS.toFloat() / SnakeGameEngine.GRID_ROWS.toFloat(), matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                .background(CyberCardBg)
                // Capture gestures smoothly via real-time pointer inputs
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            accumulatedDragX = 0f
                            accumulatedDragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDragX += dragAmount.x
                            accumulatedDragY += dragAmount.y

                            val thresholdPx = with(density) { 26.dp.toPx() }
                            if (abs(accumulatedDragX) > thresholdPx || abs(accumulatedDragY) > thresholdPx) {
                                if (abs(accumulatedDragX) > abs(accumulatedDragY)) {
                                    if (accumulatedDragX > 0) {
                                        onDirectionInput(Direction.RIGHT)
                                    } else {
                                        onDirectionInput(Direction.LEFT)
                                    }
                                } else {
                                    if (accumulatedDragY > 0) {
                                        onDirectionInput(Direction.DOWN)
                                    } else {
                                        onDirectionInput(Direction.UP)
                                    }
                                }
                                accumulatedDragX = 0f
                                accumulatedDragY = 0f
                            }
                        },
                        onDragEnd = {
                            accumulatedDragX = 0f
                            accumulatedDragY = 0f
                        }
                    )
                }
        ) {
            // Retro Graphics Canvas drawing state components
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("game_canvas")
            ) {
                val gridCols = SnakeGameEngine.GRID_COLUMNS
                val gridRows = SnakeGameEngine.GRID_ROWS
                val cellW = size.width / gridCols
                val cellH = size.height / gridRows

                // 1. Draw subtle pixel grid coordinate lines
                for (col in 1 until gridCols) {
                    drawLine(
                        color = CyberCardBorder.copy(alpha = 0.44f),
                        start = Offset(col * cellW, 0f),
                        end = Offset(col * cellW, size.height),
                        strokeWidth = 1f
                    )
                }
                for (row in 1 until gridRows) {
                    drawLine(
                        color = CyberCardBorder.copy(alpha = 0.44f),
                        start = Offset(0f, row * cellH),
                        end = Offset(size.width, row * cellH),
                        strokeWidth = 1f
                    )
                }

                // 2. Custom Draw Food Element
                val foodOffset = Offset(
                    state.food.x * cellW + cellW / 2,
                    state.food.y * cellH + cellH / 2
                )
                val foodRadius = (cellW.coerceAtMost(cellH) / 2) * 0.88f
                
                if (state.foodType == FoodType.GOLD) {
                    // Golden Cherry Glow
                    drawCircle(
                        color = NeonGold.copy(alpha = 0.18f),
                        radius = foodRadius * 2.2f,
                        center = foodOffset
                    )
                    drawCircle(
                        color = NeonGold,
                        radius = foodRadius,
                        center = foodOffset
                    )
                    // Inner shining white glint
                    drawCircle(
                        color = Color.White,
                        radius = foodRadius * 0.35f,
                        center = foodOffset - Offset(foodRadius * 0.3f, foodRadius * 0.3f)
                    )
                } else {
                    // Regular Cherry
                    drawCircle(
                        color = NeonCherry.copy(alpha = 0.16f),
                        radius = foodRadius * 1.8f,
                        center = foodOffset
                    )
                    drawCircle(
                        color = NeonCherry,
                        radius = foodRadius,
                        center = foodOffset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = foodRadius * 0.3f,
                        center = foodOffset - Offset(foodRadius * 0.3f, foodRadius * 0.3f)
                    )
                }

                // 3. Draw Snake Segment links
                state.snake.forEachIndexed { index, segment ->
                    val isHead = index == 0
                    val segmentOffset = Offset(segment.x * cellW, segment.y * cellH)
                    val insetPadding = 1.3f // Give segments a tiny separation space to look jointed
                    val segmentW = cellW - insetPadding * 2
                    val segmentH = cellH - insetPadding * 2

                    if (isHead) {
                        // Drawing head segment block
                        drawRoundRect(
                            color = NeonGreen,
                            topLeft = segmentOffset + Offset(insetPadding, insetPadding),
                            size = Size(segmentW, segmentH),
                            cornerRadius = CornerRadius(8f, 8f)
                        )

                        // Eyes placement depending on head heading direction
                        val eyeRadius = cellW * 0.11f
                        val eyeOffsetLeft: Offset
                        val eyeOffsetRight: Offset

                        when (state.direction) {
                            Direction.UP -> {
                                eyeOffsetLeft = Offset(segmentOffset.x + cellW * 0.28f, segmentOffset.y + cellH * 0.28f)
                                eyeOffsetRight = Offset(segmentOffset.x + cellW * 0.72f, segmentOffset.y + cellH * 0.28f)
                            }
                            Direction.DOWN -> {
                                eyeOffsetLeft = Offset(segmentOffset.x + cellW * 0.28f, segmentOffset.y + cellH * 0.72f)
                                eyeOffsetRight = Offset(segmentOffset.x + cellW * 0.72f, segmentOffset.y + cellH * 0.72f)
                            }
                            Direction.LEFT -> {
                                eyeOffsetLeft = Offset(segmentOffset.x + cellW * 0.28f, segmentOffset.y + cellH * 0.28f)
                                eyeOffsetRight = Offset(segmentOffset.x + cellW * 0.28f, segmentOffset.y + cellH * 0.72f)
                            }
                            Direction.RIGHT -> {
                                eyeOffsetLeft = Offset(segmentOffset.x + cellW * 0.72f, segmentOffset.y + cellH * 0.28f)
                                eyeOffsetRight = Offset(segmentOffset.x + cellW * 0.72f, segmentOffset.y + cellH * 0.72f)
                            }
                        }

                        // Eyes graphics (Black pupil, white outer iris)
                        drawCircle(color = Color.White, radius = eyeRadius, center = eyeOffsetLeft)
                        drawCircle(color = Color.White, radius = eyeRadius, center = eyeOffsetRight)
                        drawCircle(color = Color.Black, radius = eyeRadius * 0.44f, center = eyeOffsetLeft)
                        drawCircle(color = Color.Black, radius = eyeRadius * 0.44f, center = eyeOffsetRight)

                    } else {
                        // Regular body joint block with matching gradient fade toward tail
                        val progress = 1.0f - (index.toFloat() / state.snake.size)
                        val bodyColor = Color(
                            red = (0x39 * progress / 255.0f),
                            green = (0xFF * (0.4f + progress * 0.6f) / 255.0f),
                            blue = (0x14 * progress / 255.0f)
                        )
                        
                        drawRoundRect(
                            color = bodyColor,
                            topLeft = segmentOffset + Offset(insetPadding, insetPadding),
                            size = Size(segmentW, segmentH),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            // Simple conditional block instead of AnimatedVisibility to completely avoid compiler bounds clashing inside containers
            if (state.gameState == GameState.PAUSED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TransparentOverlay),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.state_paused),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = onResumeGame,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play button",
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_resume),
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Arcade D-Pad Handheld Board Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top control arrow
                IconButton(
                    onClick = { onDirectionInput(Direction.UP) },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSlate)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                        .testTag("direction_up_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Up",
                        tint = NeonGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left control arrow
                    IconButton(
                        onClick = { onDirectionInput(Direction.LEFT) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSlate)
                            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                            .testTag("direction_left_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "Left",
                            tint = NeonGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Simple center aesthetic anchor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberCardBorder)
                    )

                    // Right control arrow
                    IconButton(
                        onClick = { onDirectionInput(Direction.RIGHT) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSlate)
                            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                            .testTag("direction_right_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = "Right",
                            tint = NeonGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom control arrow
                IconButton(
                    onClick = { onDirectionInput(Direction.DOWN) },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSlate)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                        .testTag("direction_down_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Down",
                        tint = NeonGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // Modal Overlays for "Game Over" details
    if (state.gameState == GameState.GAME_OVER) {
        AlertDialog(
            onDismissRequest = {}, // Lock dialog dismissal to enforce game loops
            containerColor = CyberCardBg,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 12.dp,
            modifier = Modifier.border(1.dp, NeonCherry.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            title = {
                Text(
                    text = stringResource(R.string.state_game_over),
                    color = NeonCherry,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.final_score_label),
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${state.score}",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (state.score >= state.highHighScore && state.score > 0) {
                        Text(
                            text = "NEW PERSONAL BEST!",
                            color = NeonYellowGlow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onRestartGame,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_again_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_play_again),
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onExitToMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exit_to_menu_button")
                ) {
                    Text(
                        text = stringResource(R.string.btn_exit),
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

// Visual layout compatibility placeholder for tests & screenshots
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Hello $name!",
                color = NeonGreen,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
