package com.example.self.core.database

import androidx.room.*
import com.example.self.core.model.PomodoroSession
import com.example.self.core.model.PomodoroType
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_sessions ORDER BY completedAt DESC")
    fun getAllSessions(): Flow<List<PomodoroSession>>

    @Query("SELECT * FROM pomodoro_sessions WHERE completedAt >= :startTime AND completedAt <= :endTime")
    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<PomodoroSession>>

    @Query("SELECT * FROM pomodoro_sessions WHERE type = :type ORDER BY completedAt DESC")
    fun getSessionsByType(type: PomodoroType): Flow<List<PomodoroSession>>

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE type = 'WORK' AND completedAt >= :startOfDay")
    fun getTodayWorkSessionCount(startOfDay: Long): Flow<Int>

    @Query("SELECT SUM(durationMinutes) FROM pomodoro_sessions WHERE type = 'WORK' AND completedAt >= :startOfDay")
    fun getTodayTotalWorkMinutes(startOfDay: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PomodoroSession): Long

    @Delete
    suspend fun deleteSession(session: PomodoroSession)

    @Query("DELETE FROM pomodoro_sessions")
    suspend fun deleteAllSessions()
}
