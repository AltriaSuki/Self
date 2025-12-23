package com.example.self.feature_pomodoro

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.self.core.model.PomodoroSettings
import com.example.self.core.model.PomodoroType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = viewModel(factory = PomodoroViewModel.Factory)
) {
    val timerState by viewModel.timerState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val todaySessionCount by viewModel.todaySessionCount.collectAsState()
    val todayTotalMinutes by viewModel.todayTotalMinutes.collectAsState()
    val completedWorkSessions by viewModel.completedWorkSessions.collectAsState()
    
    var showSettings by remember { mutableStateOf(false) }
    
    val backgroundColor = when (timerState.currentType) {
        PomodoroType.WORK -> Color(0xFFE53935)
        PomodoroType.SHORT_BREAK -> Color(0xFF43A047)
        PomodoroType.LONG_BREAK -> Color(0xFF1E88E5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("番茄钟") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Session type tabs
            SessionTypeTabs(
                currentType = timerState.currentType,
                onTypeSelected = { viewModel.setSessionType(it) },
                enabled = !timerState.isRunning
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Timer circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                // Progress circle
                CircularProgressIndicator(
                    progress = { timerState.progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = backgroundColor
                )
                
                // Time display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timerState.displayTime,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = when (timerState.currentType) {
                            PomodoroType.WORK -> "专注时间"
                            PomodoroType.SHORT_BREAK -> "短休息"
                            PomodoroType.LONG_BREAK -> "长休息"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset button
                FilledTonalIconButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "重置")
                }
                
                // Play/Pause button
                FloatingActionButton(
                    onClick = {
                        if (timerState.isRunning) {
                            viewModel.pauseTimer()
                        } else {
                            viewModel.startTimer()
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    containerColor = backgroundColor
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (timerState.isRunning) "暂停" else "开始",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
                
                // Skip button
                FilledTonalIconButton(
                    onClick = { viewModel.skipToNext() },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "跳过")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Session progress indicator
            SessionProgressIndicator(
                completedSessions = completedWorkSessions,
                totalSessions = settings.sessionsBeforeLongBreak
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Today's statistics
            TodayStatisticsCard(
                sessionCount = todaySessionCount,
                totalMinutes = todayTotalMinutes
            )
        }
    }
    
    // Settings dialog
    if (showSettings) {
        PomodoroSettingsDialog(
            settings = settings,
            onDismiss = { showSettings = false },
            onConfirm = { newSettings ->
                viewModel.updateSettings(newSettings)
                showSettings = false
            }
        )
    }
}

@Composable
fun SessionTypeTabs(
    currentType: PomodoroType,
    onTypeSelected: (PomodoroType) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PomodoroType.entries.forEach { type ->
            val selected = currentType == type
            val label = when (type) {
                PomodoroType.WORK -> "专注"
                PomodoroType.SHORT_BREAK -> "短休"
                PomodoroType.LONG_BREAK -> "长休"
            }
            
            FilterChip(
                selected = selected,
                onClick = { if (enabled) onTypeSelected(type) },
                label = { Text(label) },
                enabled = enabled
            )
        }
    }
}

@Composable
fun SessionProgressIndicator(
    completedSessions: Int,
    totalSessions: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "今日进度",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        repeat(totalSessions) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < completedSessions) 
                            Color(0xFFE53935) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
fun TodayStatisticsCard(
    sessionCount: Int,
    totalMinutes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Outlined.Timer,
                value = "$sessionCount",
                label = "完成番茄"
            )
            
            VerticalDivider(
                modifier = Modifier.height(48.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            StatItem(
                icon = Icons.Outlined.Schedule,
                value = "${totalMinutes}分钟",
                label = "专注时长"
            )
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PomodoroSettingsDialog(
    settings: PomodoroSettings,
    onDismiss: () -> Unit,
    onConfirm: (PomodoroSettings) -> Unit
) {
    var workDuration by remember { mutableStateOf(settings.workDuration.toString()) }
    var shortBreakDuration by remember { mutableStateOf(settings.shortBreakDuration.toString()) }
    var longBreakDuration by remember { mutableStateOf(settings.longBreakDuration.toString()) }
    var sessionsBeforeLongBreak by remember { mutableStateOf(settings.sessionsBeforeLongBreak.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("番茄钟设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = workDuration,
                    onValueChange = { workDuration = it.filter { c -> c.isDigit() } },
                    label = { Text("专注时长（分钟）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = shortBreakDuration,
                    onValueChange = { shortBreakDuration = it.filter { c -> c.isDigit() } },
                    label = { Text("短休息时长（分钟）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = longBreakDuration,
                    onValueChange = { longBreakDuration = it.filter { c -> c.isDigit() } },
                    label = { Text("长休息时长（分钟）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = sessionsBeforeLongBreak,
                    onValueChange = { sessionsBeforeLongBreak = it.filter { c -> c.isDigit() } },
                    label = { Text("长休息间隔（番茄数）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newSettings = PomodoroSettings(
                        workDuration = workDuration.toIntOrNull() ?: 25,
                        shortBreakDuration = shortBreakDuration.toIntOrNull() ?: 5,
                        longBreakDuration = longBreakDuration.toIntOrNull() ?: 15,
                        sessionsBeforeLongBreak = sessionsBeforeLongBreak.toIntOrNull() ?: 4
                    )
                    onConfirm(newSettings)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
