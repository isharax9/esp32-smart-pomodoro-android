package com.esp32.smartpomodoro.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esp32.smartpomodoro.data.model.*
import com.esp32.smartpomodoro.ui.theme.*

@Composable
fun StopwatchCard(
    uiState: StopwatchUiState,
    mainTimeDigits: String,
    fractionDigits: String,
    onPrimaryAction: () -> Unit,
    onPrimaryActionLong: () -> Unit,
    onSecondaryAction: () -> Unit,
    onSelectSlot: (Int) -> Unit,
    onSetDelay: (Long) -> Unit,
    onToggleLock: () -> Unit,
    onUnlock: () -> Unit,
    onLockTouched: () -> Unit,
    onOpenPaceDialog: () -> Unit,
    onCopyTime: () -> Unit,
    onCopyLaps: () -> Unit,
    onClearLaps: () -> Unit,
    onResetAll: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleVolumeControl: () -> Unit
) {
    val isLocked = uiState.isTouchLocked
    val isRunning = uiState.isRunning
    val isPaused = uiState.isPaused
    val isActive = uiState.isActive
    val isCountingDown = uiState.isCountingDown

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. TOP HEADER: Multi-Stopwatch Slots (1..6) & Quick Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stopwatch Slots 1..6 Selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        uiState.slots.forEachIndexed { index, slot ->
                            val isSelected = uiState.currentSlotIndex == index
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isSelected -> AccentCyan
                                    slot.isRunning -> AccentGreen.copy(alpha = 0.25f)
                                    slot.isPaused -> AccentAmber.copy(alpha = 0.2f)
                                    else -> SurfaceDark
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isLocked) { onSelectSlot(index) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "S${slot.id}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) BackgroundDark else TextPrimary
                                    )
                                    if (slot.isRunning) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) BackgroundDark else AccentGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Toolbar actions: Volume Control, Sound, Pace, Lock
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume Keys Control Toggle
                        IconButton(
                            onClick = onToggleVolumeControl,
                            modifier = Modifier.size(32.dp),
                            enabled = !isLocked
                        ) {
                            Icon(
                                imageVector = if (uiState.volumeControlEnabled) Icons.Default.Tune else Icons.Default.VolumeMute,
                                contentDescription = "Volume Keys Control",
                                tint = if (uiState.volumeControlEnabled) AccentCyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Sound Toggle
                        IconButton(
                            onClick = onToggleSound,
                            modifier = Modifier.size(32.dp),
                            enabled = !isLocked
                        ) {
                            Icon(
                                imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "Toggle Sound",
                                tint = if (uiState.soundEnabled) AccentCyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Pace Calculator Button
                        IconButton(
                            onClick = onOpenPaceDialog,
                            modifier = Modifier.size(32.dp),
                            enabled = !isLocked && uiState.displayedTimeMs > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = "Pace Calculator",
                                tint = if (uiState.displayedTimeMs > 0) AccentCyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Touch Lock Button
                        IconButton(
                            onClick = onToggleLock,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isTouchLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock Mode",
                                tint = if (uiState.isTouchLockEnabled) AccentAmber else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Divider(color = SurfaceCardBorder, thickness = 1.dp)

                // 2. GIANT TIME READOUT
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    // Countdown delay badge if active
                    AnimatedVisibility(
                        visible = isCountingDown,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = AccentAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "PRE-START COUNTDOWN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentAmber,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Main Digits + Fraction Readout
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = mainTimeDigits,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 54.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = (-1).sp
                            ),
                            color = when {
                                isCountingDown -> AccentAmber
                                isRunning -> TextPrimary
                                isPaused -> AccentAmber
                                else -> TextSecondary
                            }
                        )

                        Text(
                            text = fractionDigits,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = when {
                                isCountingDown -> AccentAmber.copy(alpha = 0.7f)
                                isRunning -> AccentCyan
                                isPaused -> AccentAmber.copy(alpha = 0.8f)
                                else -> TextMuted
                            },
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }

                    // Status Pill (Running / Paused / Ready)
                    Surface(
                        color = when {
                            isCountingDown -> AccentAmber.copy(alpha = 0.15f)
                            isRunning -> AccentGreen.copy(alpha = 0.15f)
                            isPaused -> AccentAmber.copy(alpha = 0.15f)
                            else -> SurfaceDark
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = when {
                                isCountingDown -> "COUNTDOWN RUNNING"
                                isRunning -> "STOPWATCH ACTIVE"
                                isPaused -> "PAUSED"
                                else -> "READY"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = when {
                                isCountingDown -> AccentAmber
                                isRunning -> AccentGreen
                                isPaused -> AccentAmber
                                else -> TextMuted
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                // 3. PRE-START DELAY CHIPS (Only enabled when not active)
                if (!isActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Delay:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        listOf(0L to "None", 3000L to "3s", 5000L to "5s", 10000L to "10s").forEach { (delayMs, label) ->
                            val isSelected = uiState.currentSlot.delayTimeMs == delayMs
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) AccentCyan.copy(alpha = 0.2f) else SurfaceDark,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSetDelay(delayMs) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) AccentCyan else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // 4. MAIN ACTION BUTTONS ROW
                val isRestartScheme = uiState.controlScheme == StopwatchControlScheme.RESTART
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isRestartScheme) {
                        // Secondary Button (Reset / Lap / Delay / Restart)
                        Button(
                            onClick = onSecondaryAction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isRunning -> AccentAmber
                                    isPaused || isActive -> SurfaceDark
                                    else -> SurfaceDark
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isRunning -> if (uiState.controlScheme == StopwatchControlScheme.START_STOP_RESTART) Icons.Default.Refresh else Icons.Default.Flag
                                    isPaused || isActive -> Icons.Default.Refresh
                                    else -> Icons.Default.HourglassEmpty
                                },
                                contentDescription = "Secondary Action",
                                tint = when {
                                    isRunning -> BackgroundDark
                                    else -> TextPrimary
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isRunning -> if (uiState.controlScheme == StopwatchControlScheme.START_STOP_RESTART) "Restart" else "Lap"
                                    isPaused || isActive -> "Reset"
                                    else -> "Delay"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) BackgroundDark else TextPrimary
                            )
                        }
                    } else if (isActive) {
                        // In RESTART scheme, allow dedicated Stop & Reset button when active
                        Button(
                            onClick = onPrimaryActionLong,
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop and Reset",
                                tint = AccentRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Stop & Reset",
                                fontWeight = FontWeight.Bold,
                                color = AccentRed
                            )
                        }
                    }

                    // Primary Button (Start / Stop / Continue / Restart)
                    Button(
                        onClick = onPrimaryAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isRunning && !isRestartScheme -> AccentRed
                                isPaused -> AccentGreen
                                else -> AccentGreen
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(if (isRestartScheme && !isActive) 1f else 1.3f)
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isRunning && !isRestartScheme -> Icons.Default.Pause
                                isRestartScheme && isRunning -> Icons.Default.Refresh
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = "Primary Action"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isRestartScheme -> if (isRunning) "Restart" else "Start"
                                isRunning -> "Stop"
                                isPaused -> "Continue"
                                else -> "Start"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // 5. LAP HISTORY TABLE (if laps exist)
                if (uiState.hasLaps) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header row with Copy and Clear actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LAPS (${uiState.currentLaps.size})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = onCopyLaps,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Laps",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                TextButton(
                                    onClick = onClearLaps,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear", color = AccentRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Divider(color = SurfaceCardBorder.copy(alpha = 0.5f), thickness = 1.dp)

                        // Lap rows (latest laps at top)
                        val reversedLaps = remember(uiState.currentLaps) { uiState.currentLaps.reversed() }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            reversedLaps.take(5).forEach { lap ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                lap.isFastest -> AccentGreen.copy(alpha = 0.1f)
                                                lap.isSlowest -> AccentAmber.copy(alpha = 0.1f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "#${lap.lapNumber}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = when {
                                                lap.isFastest -> AccentGreen
                                                lap.isSlowest -> AccentAmber
                                                else -> TextSecondary
                                            }
                                        )
                                        if (lap.isFastest) {
                                            Text(
                                                text = "BEST",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                                color = AccentGreen
                                            )
                                        }
                                    }

                                    Text(
                                        text = lap.deltaFormatted,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = TextPrimary
                                    )

                                    Text(
                                        text = lap.splitFormatted,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. TOUCH LOCK OVERLAY (When locked)
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(BackgroundDark.copy(alpha = 0.88f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onLockTouched()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AccentAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = AccentAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "Touch Screen Locked",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        Text(
                            text = "Volume keys control timing:\nVol Up = Start/Stop • Vol Down = Lap",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        if (uiState.touchWarningVisible) {
                            Surface(
                                color = AccentAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Screen locked: Press Volume Up to stop or tap Unlock",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AccentAmber,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onUnlock,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Unlock", tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tap to Unlock", color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
