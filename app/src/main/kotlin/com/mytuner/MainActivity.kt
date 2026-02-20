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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import kotlin.math.cos
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
val DuoTrack = Color(0xFFDDE6F3)

private val PitchTrackDividerColor = DuoTrack
private const val PitchTrackDividerAlpha = 0.35f
private val PitchTrackDividerStrokeWidth = 1.25.dp
private val PitchTrackDividerDashLength = 6.dp
private val PitchTrackDividerDashGap = 4.dp
private val PitchTrackTraceStrokeWidth = 4.dp

private val PitchTrackGraphHeight = 148.dp
private const val PitchTrackVisibleHistoryWindowSize = 72
private const val PitchTrackXStepSampleCount = PitchTrackVisibleHistoryWindowSize
private const val PitchTrackVisibleSemitoneSpan = 12
private const val PitchTrackEdgePaddingSemitone = 1.5f
private const val PitchTrackLabelTextSizePx = 24f

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
    val isInTune = state.pitch > 0 && abs(state.centsOff) < 5
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

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

    LaunchedEffect(state.pitch) {
        if (state.isRunning) {
            history.add(state.pitch)
            while (history.size > PitchTrackVisibleHistoryWindowSize) {
                history.removeAt(0)
            }
        } else {
            history.clear()
        }
    }

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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusCard(state = state, pulseScale = pulseScale)
                        TunerCard(
                            state = state,
                            isInTune = isInTune,
                            noteScale = noteScale,
                            noteColor = noteColor,
                            cardLift = cardLift,
                            gaugeSize = 210.dp,
                            gaugeHeight = 210.dp,
                            noteFontSize = 72.sp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PitchTrackCard(
                            history = history,
                            currentNoteName = state.noteName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentModifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusCard(state = state, pulseScale = pulseScale)
                    TunerCard(
                        state = state,
                        isInTune = isInTune,
                        noteScale = noteScale,
                        noteColor = noteColor,
                        cardLift = cardLift,
                        gaugeSize = 300.dp,
                        gaugeHeight = 280.dp,
                        noteFontSize = 94.sp
                    )
                    PitchTrackCard(
                        history = history,
                        currentNoteName = state.noteName
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
                        text = state.noteName,
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
                    text = if (currentNoteName == "-") "Current --" else "Current $currentNoteName",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = DuoMuted
                )
            }
            FrequencyGraph(history = history, modifier = graphModifier)
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
fun FrequencyGraph(history: List<Float>, modifier: Modifier = Modifier) {
    val latestMidi = history.asReversed().firstNotNullOfOrNull { hzToMidi(it) }
    var targetCenterMidi by remember { mutableStateOf(latestMidi ?: 69f) }

    LaunchedEffect(latestMidi) {
        if (latestMidi != null) {
            targetCenterMidi = pitchTrackPannedCenterMidi(
                currentCenterMidi = targetCenterMidi,
                latestMidi = latestMidi,
                visibleSemitoneSpan = PitchTrackVisibleSemitoneSpan,
                edgePaddingSemitone = PitchTrackEdgePaddingSemitone
            )
        }
    }

    val animatedCenterMidi by animateFloatAsState(
        targetValue = targetCenterMidi,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "pitchTrackCenterMidi"
    )

    Canvas(
        modifier = modifier
            .border(2.dp, DuoTrack, RoundedCornerShape(16.dp))
            .background(DuoCloud, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp)
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
            textAlign = Paint.Align.RIGHT
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.NORMAL)
        }

        val graphTop = height * 0.1f
        val graphHeight = height * 0.8f
        val minMidi = animatedCenterMidi - PitchTrackVisibleSemitoneSpan / 2f
        val semitoneMarkers = pitchTrackSemitoneMarkers(
            centerMidi = animatedCenterMidi,
            visibleSemitoneSpan = PitchTrackVisibleSemitoneSpan
        )

        for (midiValue in semitoneMarkers) {
            val normalizedY = 1f - ((midiValue - minMidi) / PitchTrackVisibleSemitoneSpan)
            val y = graphTop + normalizedY * graphHeight
            drawLine(
                color = dividerColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = dividerStrokePx,
                pathEffect = dividerPathEffect
            )

            if (y in 0f..height) {
                val label = NoteUtils.calculateNoteAndCents(midiToHz(midiValue.toFloat())).first
                drawContext.canvas.nativeCanvas.drawText(label, width - 10f, y - 6f, dividerLabelPaint)
            }
        }

        val traceStrokeWidthPx = PitchTrackTraceStrokeWidth.toPx()
        if (history.isEmpty()) return@Canvas

        val step = pitchTrackXStep(width, PitchTrackXStepSampleCount)
        val strokePath = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()

        val normalizedPoints = mapFrequenciesToNormalizedY(
            frequencies = history,
            centerMidi = animatedCenterMidi,
            visibleSemitoneSpan = PitchTrackVisibleSemitoneSpan
        )
        if (normalizedPoints.isEmpty()) return@Canvas
        var started = false

        for ((index, normalizedY) in normalizedPoints) {
            val x = index * step
            val y = normalizedY * graphHeight + graphTop

            if (!started) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, y)
                started = true
            } else {
                strokePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        if (started) {
            drawContext.canvas.save()
            // 裁剪绘图区域，确保波动线（Trace）和填充色（Fill）不会超出预定的绘图主体区域（80% 高度）
            // 这样波动线就不会覆盖到顶部的标签区域或超出底部边框
            drawContext.canvas.clipRect(0f, graphTop, width, graphTop + graphHeight)

            fillPath.lineTo((history.size - 1) * step, graphTop + graphHeight)
            fillPath.lineTo(0f, graphTop + graphHeight)
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
            drawContext.canvas.restore()
        }
    }
}
