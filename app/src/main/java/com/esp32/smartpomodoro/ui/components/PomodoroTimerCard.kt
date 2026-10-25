package com.esp32.smartpomodoro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.smartpomodoro.data.model.PomodoroState
import com.esp32.smartpomodoro.data.model.PomodoroStatus
import com.esp32.smartpomodoro.ui.theme.*

@Composable
fun PomodoroTimerCard(
    status: PomodoroStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit
) {
    val phaseColor by animateColorAsState(
        targetValue = when (status.parsedState) {
            PomodoroState.WORK -> AccentRed
            PomodoroState.BREAK -> AccentGreen
            PomodoroState.IDLE -> AccentCyan
        },
        label = "phaseColor"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = status.progressFraction,
        animationSpec = tween(durationMillis = 500),
        label = "timerProgress"
    )

    val minutes = status.remainingSec / 60
    val seconds = status.remainingSec % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Phase Tag & Cycles Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phase Pill
                Surface(
                    color = phaseColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when (status.parsedState) {
                            PomodoroState.WORK -> "FOCUS SPRINT"
                            PomodoroState.BREAK -> "RECOVERY BREAK"
                            PomodoroState.IDLE -> "DESK IDLE"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = phaseColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Cycle Badge
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cycles: ${status.cyclesCompleted}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Circular Countdown Dial
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Track Background Ring
                    drawCircle(
                        color = SurfaceDark,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    // Active Progress Ring
                    val sweepAngle = animatedProgress * 360f
                    drawArc(
                        color = phaseColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 44.sp
                        ),
                        color = TextPrimary
                    )

                    if (status.isPaused) {
                        Surface(
                            color = AccentAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = if (!status.isUserPresent) "PAUSED (AWAY)" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (status.parsedState == PomodoroState.IDLE) {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(0.85f).height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Focus Sprint (25m)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Reset Button
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Pause / Resume Main Button
                    Button(
                        onClick = if (status.isPaused) onResume else onPause,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status.isPaused) AccentGreen else phaseColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(48.dp).weight(1f)
                    ) {
                        Icon(
                            imageVector = if (status.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (status.isPaused) "Resume" else "Pause"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (status.isPaused) "Resume" else "Pause",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Skip Phase Button
                    IconButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip Phase",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
