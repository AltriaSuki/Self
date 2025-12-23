package com.example.self.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: PomodoroType = PomodoroType.WORK,
    val durationMinutes: Int = 25,
    val completedAt: Long = System.currentTimeMillis(),
    val associatedTodoId: Long? = null
)

enum class PomodoroType {
    WORK,           // 工作时间
    SHORT_BREAK,    // 短休息
    LONG_BREAK      // 长休息
}

data class PomodoroSettings(
    val workDuration: Int = 25,         // 工作时长（分钟）
    val shortBreakDuration: Int = 5,    // 短休息时长（分钟）
    val longBreakDuration: Int = 15,    // 长休息时长（分钟）
    val sessionsBeforeLongBreak: Int = 4 // 长休息前的工作次数
)
