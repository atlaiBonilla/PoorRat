package com.poorrat.gotcha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_nutrition_config")
data class UserNutritionConfig(
    @PrimaryKey
    val id: Int = 1,
    val sex: String = "MALE", // "MALE" | "FEMALE"
    val weightKg: Float = 74.5f,
    val targetWeightKg: Float = 70.0f,
    val autoSyncWeight: Boolean = true,
    val heightCm: Int = 178,
    val age: Int = 26,
    val activityLevel: String = "MODERATE", // "SEDENTARY" | "LIGHT" | "MODERATE" | "HIGH"
    val goalType: String = "DEFICIT", // "DEFICIT" | "MAINTENANCE" | "SURPLUS"
    val calorieAdjustment: Int = 350, // amount to subtract or add
    val bmr: Int = 1733,
    val tdee: Int = 2686,
    val dailyCalorieTarget: Int = 2200
)
