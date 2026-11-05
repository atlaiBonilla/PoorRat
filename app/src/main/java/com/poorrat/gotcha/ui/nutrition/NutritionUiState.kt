package com.poorrat.gotcha.ui.nutrition

import com.poorrat.gotcha.data.ai.EstimatedFoodResult
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
    val remainingCalories: Int = 0,
    val progressPercent: Int = 0,
    val weekTotalCalories: Int = 0,
    val weekTargetCalories: Int = 0,
    val weekAverageCalories: Int = 0,
    val weeklyDays: List<WeeklyDayData> = emptyList(),
    
    // Bottom Sheet Visibility
    val activeSheet: ActiveBottomSheet = ActiveBottomSheet.NONE,

    // Modal 1: Daily intake add food tabs & AI
    val activeAddFoodTab: Int = 0, // 0 = Catálogo, 1 = IA, 2 = Manual
    val aiFoodPrompt: String = "",
    val isAiEstimating: Boolean = false,
    val aiEstimatedResult: EstimatedFoodResult? = null,
    val aiErrorMessage: String? = null,

    // Manual Food Form
    val manualFoodName: String = "",
    val manualGrams: String = "",
    val manualCaloriesTotal: String = "",
    val manualProteinTotal: String = "",
    val manualCarbsTotal: String = "",
    val manualFatTotal: String = "",

    // Catalog Search
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
    val latestLoggedWeight: Float = 0f,
    val calcSex: String = "MALE", // "MALE" or "FEMALE"
    val calcWeightKg: String = "",
    val calcHeightCm: String = "",
    val calcAge: String = "",
    val calcActivityLevel: String = "MODERATE", // "SEDENTARY", "LIGHT", "MODERATE", "HIGH"
    val calcGoalType: String = "DEFICIT", // "DEFICIT", "MAINTENANCE", "SURPLUS"
    val calcAdjustmentCalories: String = "",
    val previewBmr: Int = 0,
    val previewTdee: Int = 0,
    val previewTarget: Int = 0
)
