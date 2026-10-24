package com.poorrat.gotcha.ui.nutrition

import com.poorrat.gotcha.data.local.entity.DailyFoodLog
import com.poorrat.gotcha.data.local.entity.FoodItem
import com.poorrat.gotcha.data.local.entity.UserNutritionConfig

enum class ActiveBottomSheet {
    NONE,
    DAILY_INTAKE,
    WEEKLY_INTAKE,
    CALORIE_CALCULATOR
}

data class WeeklyDayData(
    val dayLabel: String, // "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"
    val dateString: String,
    val calories: Int,
    val isToday: Boolean,
    val hasLogs: Boolean
)

data class NutritionUiState(
    val config: UserNutritionConfig = UserNutritionConfig(),
    val todayLogs: List<DailyFoodLog> = emptyList(),
    val todayCalories: Int = 0,
    val todayProteinGrams: Float = 0f,
    val todayCarbsGrams: Float = 0f,
    val todayFatGrams: Float = 0f,
    val remainingCalories: Int = 2200,
    val progressPercent: Int = 0,
    val weekTotalCalories: Int = 0,
    val weekTargetCalories: Int = 15400,
    val weekAverageCalories: Int = 0,
    val weeklyDays: List<WeeklyDayData> = emptyList(),
    
    // Bottom Sheet Visibility
    val activeSheet: ActiveBottomSheet = ActiveBottomSheet.NONE,

    // Modal 1: Daily intake state
    val foodSearchQuery: String = "",
    val catalogSearchResults: List<FoodItem> = emptyList(),
    val selectedFoodItem: FoodItem? = null,
    val gramsInput: String = "120",
    val manualCaloriesPer100g: String = "",
    val manualProteinPer100g: String = "",
    val manualCarbsPer100g: String = "",
    val manualFatPer100g: String = "",

    // Modal 3: Calorie Calculator state
    val calcAutoSyncWeight: Boolean = true,
    val latestLoggedWeight: Float = 74.5f,
    val calcSex: String = "MALE", // "MALE" or "FEMALE"
    val calcWeightKg: String = "74.5",
    val calcHeightCm: String = "178",
    val calcAge: String = "26",
    val calcActivityLevel: String = "MODERATE", // "SEDENTARY", "LIGHT", "MODERATE", "HIGH"
    val calcGoalType: String = "DEFICIT", // "DEFICIT", "MAINTENANCE", "SURPLUS"
    val calcAdjustmentCalories: String = "350",
    val previewBmr: Int = 1733,
    val previewTdee: Int = 2686,
    val previewTarget: Int = 2336
)
