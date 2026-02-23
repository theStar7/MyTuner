package com.mytuner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

val DuoSky = Color(0xFFE8F4FF)
val DuoCloud = Color(0xFFFFFFFF)
val DuoGreen = Color(0xFF58CC02)
val DuoGreenDark = Color(0xFF46A302)
val DuoBlue = Color(0xFF1CB0F6)
val DuoYellow = Color(0xFFFFC800)
val DuoRed = Color(0xFFFF4B4B)
val DuoInk = Color(0xFF4B4B4B)
val DuoMuted = Color(0xFF8F9AA7)
val DuoTrack = Color(0xFFE1E1E1)

// 分割线样式：颜色、透明度、线宽和虚线长度/间隔
private val PitchTrackDividerColor = Color(0xFFE7C091)
private const val PitchTrackDividerAlpha = 0.35f
private val PitchTrackDividerStrokeWidth = 1.25.dp
private val PitchTrackDividerDashLength = 6.dp
private val PitchTrackDividerDashGap = 4.dp
// 波动线宽度
private val PitchTrackTraceStrokeWidth = 2.dp

// 图表和轨迹参数：固定高度、历史窗口、X 步长、可见半音跨度和标签字体大小
private val PitchTrackGraphHeight = 260.dp
private const val PitchTrackVisibleHistoryWindowSize = 72
private const val PitchTrackStoredHistorySampleCount = 720
private const val PitchTrackXStepSampleCount = PitchTrackVisibleHistoryWindowSize
private const val PitchTrackVisibleSemitoneSpan = 8
private const val PitchTrackEdgePaddingSemitone = 1.5f
private const val PitchTrackLabelTextSizePx = 24f
private val PitchTrackLabelZoneWidth = 56.dp
private val PitchTrackLabelGap = 6.dp
private val PitchTrackHeadGuideColor = DuoYellow.copy(alpha = 0.85f)
private val PitchTrackHeadGuideStrokeWidth = 1.5.dp

private data class AdaptiveLayoutSpec(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val cardSpacing: Dp,
    val portraitGaugeSize: Dp,
    val portraitNoteFontSize: TextUnit,
    val landscapeGaugeSize: Dp,
    val landscapeNoteFontSize: TextUnit,
    val portraitTrackHeight: Dp
)

private fun adaptiveLayoutSpec(screenWidthDp: Int, screenHeightDp: Int): AdaptiveLayoutSpec {
    val minSide = min(screenWidthDp, screenHeightDp)
    val maxSide = max(screenWidthDp, screenHeightDp)
    val compactWidth = screenWidthDp < 360
    val compactHeight = screenHeightDp < 700

    val portraitGaugeSize = when {
        compactWidth -> 216.dp
        minSide < 400 -> 240.dp
        minSide < 600 -> 280.dp
        minSide < 840 -> 340.dp
        else -> 400.dp
    }

    val portraitNoteFontSize = when {
        compactWidth -> 68.sp
        minSide < 400 -> 80.sp
        minSide < 600 -> 94.sp
        minSide < 840 -> 112.sp
        else -> 126.sp
    }

    val portraitTrackHeight = when {
        compactHeight -> 210.dp
        minSide < 400 -> 230.dp
        minSide < 600 -> PitchTrackGraphHeight
        minSide < 840 -> 320.dp
        else -> 380.dp
    }

    val landscapeGaugeSize = when {
        minSide < 430 -> 180.dp
        maxSide < 900 -> 210.dp
        maxSide < 1200 -> 280.dp
        else -> 340.dp
    }

    val landscapeNoteFontSize = when {
        minSide < 430 -> 60.sp
        maxSide < 900 -> 72.sp
        maxSide < 1200 -> 90.sp
        else -> 104.sp
    }

    return AdaptiveLayoutSpec(
        horizontalPadding = when {
            compactWidth -> 12.dp
            screenWidthDp >= 1200 -> 36.dp
            screenWidthDp >= 900 -> 28.dp
            else -> 20.dp
        },
        verticalPadding = when {
            compactHeight -> 14.dp
            screenHeightDp >= 1200 -> 36.dp
            screenHeightDp >= 900 -> 28.dp
            else -> 24.dp
        },
        cardSpacing = when {
            compactHeight -> 10.dp
            minSide >= 900 -> 20.dp
            minSide >= 600 -> 16.dp
            else -> 14.dp
        },
        portraitGaugeSize = portraitGaugeSize,
        portraitNoteFontSize = portraitNoteFontSize,
        landscapeGaugeSize = landscapeGaugeSize,
        landscapeNoteFontSize = landscapeNoteFontSize,
        portraitTrackHeight = portraitTrackHeight
    )
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermission()

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
fun MainScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        PitchScreen()
    }
}

@Composable
fun PitchScreen(vm: PitchViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val history = remember { mutableStateListOf<Float>() }
    var historyOffsetSamples by remember { mutableStateOf(0) }
    var verticalPanOffsetSemitone by remember { mutableStateOf(0f) }
    val isInTune = state.pitch > 0 && abs(state.centsOff) < 5
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val usePortraitScroll = configuration.screenHeightDp < 760
    val layoutSpec = adaptiveLayoutSpec(configuration.screenWidthDp, configuration.screenHeightDp)
    val portraitScrollState = rememberScrollState()

    val noteScale by animateFloatAsState(
        targetValue = if (isInTune) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f),
        label = "noteScale"
    )
    val noteColor by animateColorAsState(
        targetValue = if (isInTune) DuoGreen else DuoInk,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "noteColor"
    )
    val cardLift by animateDpAsState(
        targetValue = if (state.isRunning) 0.dp else 2.dp,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "cardLift"
    )
    val controlColor by animateColorAsState(
        targetValue = if (state.isRunning) DuoRed else DuoGreen,
        animationSpec = tween(220),
        label = "controlColor"
    )

    val pulseTransition = rememberInfiniteTransition(label = "listenPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(state.pitch, state.isRunning) {
        if (state.isRunning) {
            history.add(state.pitch)
            while (history.size > PitchTrackStoredHistorySampleCount) {
                history.removeAt(0)
            }
            historyOffsetSamples = 0
            verticalPanOffsetSemitone = 0f
        }
    }

    LaunchedEffect(state.isRunning) {
        if (!state.isRunning) {
            historyOffsetSamples = pitchTrackPanMinOffset(PitchTrackVisibleHistoryWindowSize)
        }
    }

    val panMinOffset = pitchTrackPanMinOffset(PitchTrackVisibleHistoryWindowSize)
    val panMaxOffset = pitchTrackPanMaxOffset(
        historySize = history.size,
        visibleSampleCount = PitchTrackVisibleHistoryWindowSize
    )

    LaunchedEffect(history.size, state.isRunning) {
        if (!state.isRunning) {
            val clampedOffset = historyOffsetSamples.coerceIn(panMinOffset, panMaxOffset)
            if (historyOffsetSamples != clampedOffset) {
                historyOffsetSamples = clampedOffset
            }
        }
    }

    var pausedMarkerNoteName by remember { mutableStateOf("-") }
    val tunerDisplayedNoteName = if (state.isRunning) state.noteName else pausedMarkerNoteName

    Surface(modifier = Modifier.fillMaxSize(), color = DuoSky) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = DuoBlue.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.38f,
                    center = Offset(size.width * 0.15f, size.height * 0.1f)
                )
                drawCircle(
                    color = DuoYellow.copy(alpha = 0.14f),
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.86f, size.height * 0.9f)
                )
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = layoutSpec.horizontalPadding,
                            vertical = layoutSpec.verticalPadding
                        ),
                    horizontalArrangement = Arrangement.spacedBy(layoutSpec.cardSpacing)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(layoutSpec.cardSpacing)
                    ) {
                        StatusCard(state = state, pulseScale = pulseScale)
                        TunerCard(
                            state = state,
                            displayedNoteName = tunerDisplayedNoteName,
                            isInTune = isInTune,
                            noteScale = noteScale,
                            noteColor = noteColor,
                            cardLift = cardLift,
                            gaugeSize = layoutSpec.landscapeGaugeSize,
                            gaugeHeight = layoutSpec.landscapeGaugeSize,
                            noteFontSize = layoutSpec.landscapeNoteFontSize
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(layoutSpec.cardSpacing)
                    ) {
                        PitchTrackCard(
                            history = history,
                            currentNoteName = state.noteName,
                            isRunning = state.isRunning,
                            historyOffsetSamples = historyOffsetSamples,
                            onHistoryOffsetSamplesChange = { historyOffsetSamples = it },
                            verticalPanOffsetSemitone = verticalPanOffsetSemitone,
                            onVerticalPanOffsetSemitoneChange = { verticalPanOffsetSemitone = it },
                            onPausedMarkerNoteNameChange = { pausedMarkerNoteName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentModifier = Modifier
                                .fillMaxSize()
                                .padding(layoutSpec.cardSpacing - 2.dp),
                            graphModifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        ControlButton(
                            isRunning = state.isRunning,
                            controlColor = controlColor,
                            onToggle = { vm.toggleEngine() }
                        )
                    }
                }
            } else {
                val portraitContainerModifier = if (usePortraitScroll) {
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(portraitScrollState)
                } else {
                    Modifier.fillMaxSize()
                }

                Column(
                    modifier = portraitContainerModifier
                        .padding(
                            horizontal = layoutSpec.horizontalPadding,
                            vertical = layoutSpec.verticalPadding
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(layoutSpec.cardSpacing)
                ) {
                    StatusCard(state = state, pulseScale = pulseScale)
                    TunerCard(
                        state = state,
                        displayedNoteName = tunerDisplayedNoteName,
                        isInTune = isInTune,
                        noteScale = noteScale,
                        noteColor = noteColor,
                        cardLift = cardLift,
                        gaugeSize = layoutSpec.portraitGaugeSize,
                        gaugeHeight = layoutSpec.portraitGaugeSize,
                        noteFontSize = layoutSpec.portraitNoteFontSize
                    )
                    PitchTrackCard(
                        history = history,
                        currentNoteName = state.noteName,
                        isRunning = state.isRunning,
                        historyOffsetSamples = historyOffsetSamples,
                        onHistoryOffsetSamplesChange = { historyOffsetSamples = it },
                        verticalPanOffsetSemitone = verticalPanOffsetSemitone,
                        onVerticalPanOffsetSemitoneChange = { verticalPanOffsetSemitone = it },
                        onPausedMarkerNoteNameChange = { pausedMarkerNoteName = it },
                        modifier = if (usePortraitScroll) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        },
                        contentModifier = if (usePortraitScroll) {
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        } else {
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        },
                        graphModifier = if (usePortraitScroll) {
                            Modifier
                                .fillMaxWidth()
                                .height(layoutSpec.portraitTrackHeight)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        }
                    )
                    ControlButton(
                        isRunning = state.isRunning,
                        controlColor = controlColor,
                        onToggle = { vm.toggleEngine() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: PitchState, pulseScale: Float) {
    Surface(
        color = DuoCloud,
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MyTuner",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                ),
                color = DuoInk
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .graphicsLayer {
                            val runningPulse = if (state.isRunning) pulseScale else 1f
                            scaleX = runningPulse
                            scaleY = runningPulse
                        }
                        .background(
                            if (state.isRunning) DuoGreen else DuoMuted,
                            CircleShape
                        )
                )
                Text(
                    text = if (state.isRunning) "LISTENING" else "READY",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (state.isRunning) DuoGreenDark else DuoMuted
                )
            }
        }
    }
}

@Composable
private fun TunerCard(
    state: PitchState,
    displayedNoteName: String,
    isInTune: Boolean,
    noteScale: Float,
    noteColor: Color,
    cardLift: Dp,
    gaugeSize: Dp,
    gaugeHeight: Dp,
    noteFontSize: TextUnit
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = DuoCloud,
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = cardLift)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gaugeHeight)
            ) {
                TunerGauge(
                    centsOff = state.centsOff.toFloat(),
                    isInTune = isInTune,
                    modifier = Modifier.size(gaugeSize)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayedNoteName,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = noteFontSize
                        ),
                        color = noteColor,
                        modifier = Modifier.graphicsLayer {
                            scaleX = noteScale
                            scaleY = noteScale
                        }
                    )

                    if (state.pitch > 0) {
                        Text(
                            text = "${if (state.centsOff > 0) "+" else ""}${state.centsOff} cents",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isInTune) DuoGreen else DuoBlue,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isInTune,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Surface(
                            color = DuoGreen,
                            shape = RoundedCornerShape(50),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = "IN TUNE!",
                                color = DuoCloud,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PitchTrackCard(
    history: List<Float>,
    currentNoteName: String,
    isRunning: Boolean,
    historyOffsetSamples: Int,
    onHistoryOffsetSamplesChange: (Int) -> Unit,
    verticalPanOffsetSemitone: Float,
    onVerticalPanOffsetSemitoneChange: (Float) -> Unit,
    onPausedMarkerNoteNameChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    contentModifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
    graphModifier: Modifier = Modifier
        .fillMaxWidth()
        .height(PitchTrackGraphHeight)
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = DuoCloud,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Column(modifier = contentModifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pitch Track",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = DuoInk
                )
                Text(
                    text = when {
                        !isRunning && history.isNotEmpty() -> "Paused - Drag to pan"
                        currentNoteName == "-" -> "Current --"
                        else -> "Current $currentNoteName"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = DuoMuted
                )
            }
            FrequencyGraph(
                history = history,
                isRunning = isRunning,
                historyOffsetSamples = historyOffsetSamples,
                onHistoryOffsetSamplesChange = onHistoryOffsetSamplesChange,
                verticalPanOffsetSemitone = verticalPanOffsetSemitone,
                onVerticalPanOffsetSemitoneChange = onVerticalPanOffsetSemitoneChange,
                onPausedMarkerNoteNameChange = onPausedMarkerNoteNameChange,
                modifier = graphModifier
            )
        }
    }
}

@Composable
private fun ControlButton(
    isRunning: Boolean,
    controlColor: Color,
    onToggle: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .offset(y = 6.dp)
                .background(
                    if (isRunning) DuoRed.copy(alpha = 0.75f) else DuoGreenDark,
                    RoundedCornerShape(18.dp)
                )
        )

        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(containerColor = controlColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text(
                text = if (isRunning) "STOP LISTENING" else "START TUNING",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
fun TunerGauge(centsOff: Float, isInTune: Boolean, modifier: Modifier = Modifier) {
    val animatedCents by animateFloatAsState(
        targetValue = centsOff,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 240f),
        label = "gauge"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 18f
        val startAngle = 150f
        val sweepAngle = 240f

        drawArc(
            color = DuoTrack,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 24f, cap = StrokeCap.Round)
        )

        drawArc(
            color = DuoGreen.copy(alpha = 0.33f),
            startAngle = 264f,
            sweepAngle = 12f,
            useCenter = false,
            style = Stroke(width = 24f, cap = StrokeCap.Round)
        )

        drawArc(
            color = DuoMuted.copy(alpha = 0.35f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(
                width = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 10f))
            )
        )

        val angleOffset = animatedCents.coerceIn(-50f, 50f) * 2.4f
        val indicatorAngleRad = Math.toRadians((270f + angleOffset).toDouble())

        val needleLength = radius - 26f
        val endX = center.x + needleLength * cos(indicatorAngleRad).toFloat()
        val endY = center.y + needleLength * sin(indicatorAngleRad).toFloat()

        drawLine(
            color = if (isInTune) DuoGreen else DuoBlue,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )

        drawCircle(color = DuoCloud, radius = 16f, center = center)
        drawCircle(color = if (isInTune) DuoGreen else DuoBlue, radius = 9f, center = center)
    }
}

@Composable
fun FrequencyGraph(
    history: List<Float>,
    isRunning: Boolean,
    historyOffsetSamples: Int,
    onHistoryOffsetSamplesChange: (Int) -> Unit,
    verticalPanOffsetSemitone: Float,
    onVerticalPanOffsetSemitoneChange: (Float) -> Unit,
    onPausedMarkerNoteNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val labelZoneWidthPx = with(density) { PitchTrackLabelZoneWidth.toPx() }
    val labelGapPx = with(density) { PitchTrackLabelGap.toPx() }
    val maxOffset = pitchTrackMaxOffset(
        historySize = history.size,
        visibleSampleCount = PitchTrackVisibleHistoryWindowSize
    )
    val panMinOffset = pitchTrackPanMinOffset(PitchTrackVisibleHistoryWindowSize)
    val panMaxOffset = pitchTrackPanMaxOffset(
        historySize = history.size,
        visibleSampleCount = PitchTrackVisibleHistoryWindowSize
    )
    val boundedHistoryOffset = historyOffsetSamples.coerceIn(0, maxOffset)
    val historyOverflowOffset = historyOffsetSamples - boundedHistoryOffset
    val visibleHistory = pitchTrackVisibleWindow(
        frequencies = history,
        visibleSampleCount = PitchTrackVisibleHistoryWindowSize,
        offsetFromLatest = if (isRunning) 0 else boundedHistoryOffset
    )
    val latestMidi = visibleHistory.asReversed().firstNotNullOfOrNull { hzToMidi(it) }
    var targetCenterMidi by remember { mutableStateOf(latestMidi ?: 69f) }

    LaunchedEffect(latestMidi, isRunning) {
        targetCenterMidi = pitchTrackTargetCenterMidi(
            currentCenterMidi = targetCenterMidi,
            latestMidi = latestMidi,
            isRunning = isRunning,
            visibleSemitoneSpan = PitchTrackVisibleSemitoneSpan,
            edgePaddingSemitone = PitchTrackEdgePaddingSemitone
        )
    }

    val animatedCenterMidi by animateFloatAsState(
        targetValue = targetCenterMidi,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "pitchTrackCenterMidi"
    )
    val displayedCenterMidi = if (isRunning) {
        animatedCenterMidi
    } else {
        targetCenterMidi + verticalPanOffsetSemitone
    }

    val canPanHistory = !isRunning && history.isNotEmpty()
    val latestHistoryOffset by rememberUpdatedState(newValue = historyOffsetSamples.coerceIn(panMinOffset, panMaxOffset))
    val latestOnHistoryOffsetSamplesChange by rememberUpdatedState(newValue = onHistoryOffsetSamplesChange)
    val latestVerticalPanOffsetSemitone by rememberUpdatedState(newValue = verticalPanOffsetSemitone)
    val latestOnVerticalPanOffsetSemitoneChange by rememberUpdatedState(newValue = onVerticalPanOffsetSemitoneChange)
    var graphSize by remember { mutableStateOf(IntSize.Zero) }

    val calcTraceWidth = (graphSize.width.toFloat() - labelZoneWidthPx).coerceAtLeast(0f)
    val calcStep = pitchTrackXStep(calcTraceWidth, PitchTrackXStepSampleCount.coerceAtLeast(1))
    val lastValidIndex = visibleHistory.indexOfLast { it > 0f }
    val calcUsedWidth = (lastValidIndex.coerceAtLeast(0) * calcStep).coerceAtMost(calcTraceWidth)
    val calcWaveAnchorX = pitchTrackWaveAnchorX(waveWidth = calcTraceWidth, isRunning = isRunning)
    val calcPauseCompensationPx = if (!isRunning) {
        pitchTrackPauseCompensationPx(calcTraceWidth)
    } else {
        0f
    }
    val calcRelativeOverflowOffset = if (isRunning) 0 else historyOverflowOffset - panMinOffset
    val calcTrailingOffset = calcWaveAnchorX - calcUsedWidth + (calcRelativeOverflowOffset * calcStep) + calcPauseCompensationPx
    val calcMarkerX = pitchTrackTimeMarkerX(waveWidth = calcTraceWidth).coerceIn(0f, calcTraceWidth)
    val pausedMarkerNoteName = pitchTrackMarkerFrequencyAtX(
        frequencies = visibleHistory,
        markerX = calcMarkerX,
        trailingOffsetPx = calcTrailingOffset,
        stepPx = calcStep
    )?.let { NoteUtils.calculateNoteAndCents(it).first } ?: "-"

    LaunchedEffect(isRunning, pausedMarkerNoteName) {
        if (!isRunning) {
            onPausedMarkerNoteNameChange(pausedMarkerNoteName)
        }
    }

    val panModifier = if (canPanHistory) {
        Modifier.pointerInput(isRunning, panMinOffset, panMaxOffset) {
            var dragCarryPx = 0f
            var gestureOffset = latestHistoryOffset
            var gestureVerticalPanOffset = latestVerticalPanOffsetSemitone
            detectDragGestures(
                onDragStart = {
                    dragCarryPx = 0f
                    gestureOffset = latestHistoryOffset
                    gestureVerticalPanOffset = latestVerticalPanOffsetSemitone
                },
                onDrag = { change, dragAmount ->
                    change.consume()

                    val canvasWidth = size.width.toFloat()
                    val traceWidth = (canvasWidth - labelZoneWidthPx).coerceAtLeast(0f)
                    val stepPx = pitchTrackXStep(
                        width = traceWidth,
                        sampleCount = PitchTrackXStepSampleCount
                    )
                    val nextPanState = pitchTrackNextPanOffset(
                        currentOffset = gestureOffset,
                        dragAmountPx = dragAmount.x,
                        stepPx = stepPx,
                        minOffset = panMinOffset,
                        maxOffset = panMaxOffset,
                        carryPx = dragCarryPx
                    )

                    dragCarryPx = nextPanState.carryPx
                    if (nextPanState.offset != gestureOffset) {
                        gestureOffset = nextPanState.offset
                        latestOnHistoryOffsetSamplesChange(nextPanState.offset)
                    }

                    val nextVerticalPanOffset = pitchTrackNextVerticalPanOffset(
                        currentOffsetSemitone = gestureVerticalPanOffset,
                        dragAmountPx = dragAmount.y,
                        graphHeightPx = size.height.toFloat(),
                        visibleSemitoneSpan = PitchTrackVisibleSemitoneSpan,
                        maxAbsOffsetSemitone = Float.MAX_VALUE
                    )
                    if (abs(nextVerticalPanOffset - gestureVerticalPanOffset) > 0.0001f) {
                        gestureVerticalPanOffset = nextVerticalPanOffset
                        latestOnVerticalPanOffsetSemitoneChange(nextVerticalPanOffset)
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .then(panModifier)
            .onSizeChanged { graphSize = it }
            .border(2.dp, DuoTrack, RoundedCornerShape(16.dp))
            .background(DuoCloud, RoundedCornerShape(16.dp))
    ) {
        val width = size.width
        val height = size.height

        val dividerStrokePx = PitchTrackDividerStrokeWidth.toPx()
        val dashLenPx = PitchTrackDividerDashLength.toPx()
        val dashGapPx = PitchTrackDividerDashGap.toPx()
        val dividerPathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLenPx, dashGapPx), 0f)
        val dividerColor = PitchTrackDividerColor.copy(alpha = PitchTrackDividerAlpha)
        val dividerLabelPaint = Paint().apply {
            color = DuoMuted.toArgb()
            textSize = PitchTrackLabelTextSizePx
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.NORMAL)
        }

        val graphTop = 0f
        val graphHeight = height
        val traceWidth = (width - labelZoneWidthPx).coerceAtLeast(0f)
        val dividerRight = traceWidth
        val minMidi = displayedCenterMidi - PitchTrackVisibleSemitoneSpan / 2f
        val semitoneMarkers = pitchTrackSemitoneMarkers(
            centerMidi = displayedCenterMidi,
            visibleSemitoneSpan = PitchTrackVisibleSemitoneSpan
        )

        for (midiValue in semitoneMarkers) {
            val normalizedY = 1f - ((midiValue - minMidi) / PitchTrackVisibleSemitoneSpan)
            val y = graphTop + normalizedY * graphHeight
            drawLine(
                color = dividerColor,
                start = Offset(0f, y),
                end = Offset(dividerRight, y),
                strokeWidth = dividerStrokePx,
                pathEffect = dividerPathEffect
            )

            if (y in 0f..height) {
                val label = NoteUtils.calculateNoteAndCents(midiToHz(midiValue.toFloat())).first
                drawContext.canvas.nativeCanvas.drawText(label, dividerRight + labelGapPx, y - 6f, dividerLabelPaint)
            }
        }

        val traceStrokeWidthPx = PitchTrackTraceStrokeWidth.toPx()
        val headGuideStrokeWidthPx = PitchTrackHeadGuideStrokeWidth.toPx()
        if (visibleHistory.isEmpty()) return@Canvas

        val step = pitchTrackXStep(
            traceWidth,
            PitchTrackXStepSampleCount.coerceAtLeast(1)
        )

        val normalizedPoints = mapFrequenciesToNormalizedY(
            frequencies = visibleHistory,
            centerMidi = displayedCenterMidi,
            visibleSemitoneSpan = PitchTrackVisibleSemitoneSpan
        )
        if (normalizedPoints.isEmpty()) return@Canvas
        val lastDataIndex = (normalizedPoints.maxOfOrNull { it.index } ?: 0)
        val usedWidth = (lastDataIndex * step).coerceAtMost(traceWidth)
        val segments = pitchTrackContinuousSegments(normalizedPoints)
        if (segments.isEmpty()) return@Canvas
        val waveLeft = 0f
        val waveTop = graphTop
        val waveRight = traceWidth
        val waveBottom = graphTop + graphHeight
        val waveAnchorX = pitchTrackWaveAnchorX(waveWidth = waveRight, isRunning = isRunning)
        val pauseCompensationPx = if (!isRunning) {
            pitchTrackPauseCompensationPx(waveRight)
        } else {
            0f
        }
        val timeMarkerX = pitchTrackTimeMarkerX(waveWidth = waveRight).coerceIn(waveLeft, waveRight)
        val relativeOverflowOffset = if (isRunning) 0 else historyOverflowOffset - panMinOffset
        val trailingOffset = waveAnchorX - usedWidth + (relativeOverflowOffset * step) + pauseCompensationPx

        drawContext.canvas.save()
        // 裁剪绘图区域，确保波动线和填充不会超出绘图区边界
        drawContext.canvas.clipRect(waveLeft, waveTop, waveRight, waveBottom)

        for (segment in segments) {
            if (segment.isEmpty()) continue

            val firstSample = segment.first()
            val firstX = (firstSample.index * step + trailingOffset).coerceIn(waveLeft, waveRight)
            val firstY = firstSample.normalizedY * graphHeight + graphTop

            val strokePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(firstX, firstY)
            }
            val fillPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(firstX, firstY)
            }

            var lastX = firstX
            for (pointIndex in 1 until segment.size) {
                val sample = segment[pointIndex]
                val x = (sample.index * step + trailingOffset).coerceIn(waveLeft, waveRight)
                val y = sample.normalizedY * graphHeight + graphTop
                strokePath.lineTo(x, y)
                fillPath.lineTo(x, y)
                lastX = x
            }

            fillPath.lineTo(lastX, waveBottom)
            fillPath.lineTo(firstX, waveBottom)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(DuoBlue.copy(alpha = 0.35f), Color.Transparent)
                )
            )

            drawPath(
                path = strokePath,
                color = DuoBlue,
                style = Stroke(width = traceStrokeWidthPx, cap = StrokeCap.Round)
            )
        }

        drawLine(
            color = PitchTrackHeadGuideColor,
            start = Offset(timeMarkerX, waveTop),
            end = Offset(timeMarkerX, waveBottom),
            strokeWidth = headGuideStrokeWidthPx
        )

        drawContext.canvas.restore()
    }
}
