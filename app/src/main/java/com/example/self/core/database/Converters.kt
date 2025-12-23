package com.example.self.core.database

import androidx.room.TypeConverter
import com.example.self.core.model.PomodoroType
import com.example.self.core.model.Priority

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter
    fun fromPomodoroType(type: PomodoroType): String = type.name

    @TypeConverter
    fun toPomodoroType(value: String): PomodoroType = PomodoroType.valueOf(value)
}
