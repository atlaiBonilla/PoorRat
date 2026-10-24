package com.poorrat.gotcha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_session_logs")
data class WorkoutSessionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: Int,
    val durationSeconds: Long,
    val timestampMillis: Long = System.currentTimeMillis()
)
