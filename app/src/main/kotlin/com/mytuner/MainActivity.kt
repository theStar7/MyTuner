package com.mytuner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Design System Colors
val DarkBackground = Color(0xFF121212)
val SurfaceColor = Color(0xFF1E293B)
val PrimaryBlue = Color(0xFF3B82F6)
val SecondaryBlue = Color(0xFF60A5FA)
val AccentOrange = Color(0xFFF97316)
val SuccessGreen = Color(0xFF22C55E)
val ErrorRed = Color(0xFFEF4444)
val TextLight = Color(0xFFF8FAFC)
val TextDim = Color(0xFF94A3B8)

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // 权限处理
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
            != PackageManager.PERMISSION_GRANTED) {
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

    LaunchedEffect(state.pitch) {
        if (state.isRunning) {
            history.add(state.pitch)
            if (history.size > 100) history.removeAt(0)
        } else {
            history.clear()
        }
    }

    // Main Container
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Text(
                text = "MyTuner",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = PrimaryBlue
            )

            // Tuner Visualization
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                TunerGauge(centsOff = state.centsOff.toFloat())
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.noteName,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 96.sp
                        ),
                        color = if (abs(state.centsOff) < 5) SuccessGreen else TextLight
                    )
                    Text(
                        text = if (state.pitch > 0) "%.1f Hz".format(state.pitch) else "-- Hz",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextDim
                    )
                    if (state.pitch > 0) {
                        Text(
                            text = "${if (state.centsOff > 0) "+" else ""}${state.centsOff} cents",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (abs(state.centsOff) < 5) SuccessGreen else AccentOrange,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Frequency History Graph
            FrequencyGraph(history = history)

            // Controls
            Button(
                onClick = { vm.toggleEngine() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRunning) ErrorRed else AccentOrange
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (state.isRunning) "STOP LISTENING" else "START TUNING",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun TunerGauge(centsOff: Float) {
    val animatedCents by animateFloatAsState(targetValue = centsOff, label = "gauge")
    
    Canvas(modifier = Modifier.size(300.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 20f
        val startAngle = 150f
        val sweepAngle = 240f
        
        // Background Arc
        drawArc(
            color = SurfaceColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 20f, cap = StrokeCap.Round)
        )

        // Center Marker (In-tune zone)
        val centerAngle = 270f // Top center (150 + 240/2 = 270)
        val markerSweep = 10f
        drawArc(
            color = SuccessGreen.copy(alpha = 0.3f),
            startAngle = centerAngle - markerSweep/2,
            sweepAngle = markerSweep,
            useCenter = false,
            style = Stroke(width = 20f, cap = StrokeCap.Round)
        )

        // Indicator Needle
        // Map -50..+50 cents to 150..390 degrees (270 center)
        // Range 100 cents = 240 degrees -> 1 cent = 2.4 degrees
        val angleOffset = (animatedCents.coerceIn(-50f, 50f) * 2.4f)
        val indicatorAngleRad = Math.toRadians((centerAngle + angleOffset).toDouble())
        
        val needleLength = radius - 30f
        val endX = center.x + needleLength * cos(indicatorAngleRad).toFloat()
        val endY = center.y + needleLength * sin(indicatorAngleRad).toFloat()

        drawLine(
            color = if (abs(animatedCents) < 5) SuccessGreen else AccentOrange,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
        
        // Center Pivot
        drawCircle(
            color = TextLight,
            radius = 12f,
            center = center
        )
    }
}

@Composable
fun FrequencyGraph(history: List<Float>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(SurfaceColor, RoundedCornerShape(12.dp))
    ) {
        val width = size.width
        val height = size.height
        if (history.isEmpty()) return@Canvas

        val step = width / 100f
        val strokePath = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()
        
        // Find min/max for scaling
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

        // Close fill path
        if (started) {
            fillPath.lineTo((history.size - 1) * step, height)
            fillPath.lineTo(0f, height)
            fillPath.close()

            // Draw Fill First
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(SecondaryBlue.copy(alpha = 0.3f), Color.Transparent)
                )
            )

            // Draw Stroke Second
            drawPath(
                path = strokePath,
                color = SecondaryBlue,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }
    }
}
