package com.poorrat.gotcha.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["timestampMillis"])]
)
data class WeightLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weightKg: Float,
    val note: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val dateString: String // YYYY-MM-DD
)
