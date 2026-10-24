package com.poorrat.gotcha.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_food_logs",
    indices = [Index(value = ["dateString"])]
)
data class DailyFoodLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val foodName: String,
    val gramsConsumed: Float,
    val calories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val timestampMillis: Long = System.currentTimeMillis(),
    val dateString: String // YYYY-MM-DD
)
