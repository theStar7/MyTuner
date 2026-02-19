package com.mytuner

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
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
            if (history.size > 100) history.removeAt(0)
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
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
                                .height(280.dp)
                        ) {
                            TunerGauge(centsOff = state.centsOff.toFloat(), isInTune = isInTune)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.noteName,
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 94.sp
                                    ),
                                    color = noteColor,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = noteScale
                                        scaleY = noteScale
                                    }
                                )
                                Text(
                                    text = if (state.pitch > 0) "%.1f Hz".format(state.pitch) else "-- Hz",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DuoMuted
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

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DuoCloud,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Pitch Track",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = DuoInk,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        FrequencyGraph(history = history)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .offset(y = 6.dp)
                            .background(
                                if (state.isRunning) DuoRed.copy(alpha = 0.75f) else DuoGreenDark,
                                RoundedCornerShape(18.dp)
                            )
                    )

                    Button(
                        onClick = { vm.toggleEngine() },
                        colors = ButtonDefaults.buttonColors(containerColor = controlColor),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Text(
                            text = if (state.isRunning) "STOP LISTENING" else "START TUNING",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TunerGauge(centsOff: Float, isInTune: Boolean) {
    val animatedCents by animateFloatAsState(
        targetValue = centsOff,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 240f),
        label = "gauge"
    )

    Canvas(modifier = Modifier.size(300.dp)) {
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
fun FrequencyGraph(history: List<Float>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .border(2.dp, DuoTrack, RoundedCornerShape(16.dp))
            .background(DuoCloud, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height
        if (history.isEmpty()) return@Canvas

        val step = width / 100f
        val strokePath = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()

        val validPoints = history.filter { it > 0 }
        if (validPoints.isEmpty()) return@Canvas

        val minHz = validPoints.minOrNull() ?: 0f
        val maxHz = validPoints.maxOrNull() ?: 1000f
        val range = (maxHz - minHz).coerceAtLeast(10f)

        var started = false

        for (i in history.indices) {
            val freq = history[i]
            if (freq <= 0) continue

            val x = i * step
            val normalizedY = 1f - ((freq - minHz) / range)
            val y = normalizedY * height * 0.8f + height * 0.1f

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
            fillPath.lineTo((history.size - 1) * step, height)
            fillPath.lineTo(0f, height)
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
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }
    }
}
