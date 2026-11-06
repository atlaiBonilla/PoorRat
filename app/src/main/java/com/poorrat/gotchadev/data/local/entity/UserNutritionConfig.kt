package com.poorrat.gotchadev.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_nutrition_config")
data class UserNutritionConfig(
    @PrimaryKey
    val id: Int = 1,
    val sex: String = "MALE", // "MALE" | "FEMALE"
    val weightKg: Float = 0f,
    val targetWeightKg: Float = 0f,
    val autoSyncWeight: Boolean = true,
    val heightCm: Int = 0,
    val age: Int = 0,
    val activityLevel: String = "MODERATE", // "SEDENTARY" | "LIGHT" | "MODERATE" | "HIGH"
    val goalType: String = "MAINTENANCE", // "DEFICIT" | "MAINTENANCE" | "SURPLUS"
    val calorieAdjustment: Int = 0, // amount to subtract or add
    val bmr: Int = 0,
    val tdee: Int = 0,
    val dailyCalorieTarget: Int = 0
)
