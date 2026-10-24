package com.poorrat.gotcha.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poorrat.gotcha.data.local.entity.DailyFoodLog
import com.poorrat.gotcha.data.local.entity.FoodItem
import com.poorrat.gotcha.data.local.entity.UserNutritionConfig
import com.poorrat.gotcha.data.local.entity.WeightLog
import com.poorrat.gotcha.data.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private data class NutritionDatabaseData(
    val config: UserNutritionConfig,
    val todayLogs: List<DailyFoodLog>,
    val weekLogs: List<DailyFoodLog>,
    val latestWeight: WeightLog?
)

private data class NutritionFormInputs(
    val activeSheet: ActiveBottomSheet,
    val foodSearchQuery: String,
    val selectedFoodItem: FoodItem?,
    val gramsInput: String,
    val catalogSearchResults: List<FoodItem>
)

class NutritionViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val today = LocalDate.now()
    private val todayString = today.format(dateFormatter)
    private val mondayDate = today.with(DayOfWeek.MONDAY).format(dateFormatter)
    private val sundayDate = today.with(DayOfWeek.SUNDAY).format(dateFormatter)

    private val _activeSheet = MutableStateFlow(ActiveBottomSheet.NONE)
    private val _foodSearchQuery = MutableStateFlow("")
    private val _selectedFoodItem = MutableStateFlow<FoodItem?>(null)
    private val _gramsInput = MutableStateFlow("120")
    private val _manualCaloriesPer100g = MutableStateFlow("")
    private val _manualProteinPer100g = MutableStateFlow("")
    private val _manualCarbsPer100g = MutableStateFlow("")
    private val _manualFatPer100g = MutableStateFlow("")

    // Calculator inputs
    private val _calcAutoSyncWeight = MutableStateFlow(true)
    private val _calcSex = MutableStateFlow("MALE")
    private val _calcWeightKg = MutableStateFlow("74.5")
    private val _calcHeightCm = MutableStateFlow("178")
    private val _calcAge = MutableStateFlow("26")
    private val _calcActivityLevel = MutableStateFlow("MODERATE")
    private val _calcGoalType = MutableStateFlow("DEFICIT")
    private val _calcAdjustmentCalories = MutableStateFlow("350")

    private val _catalogSearchResults = MutableStateFlow<List<FoodItem>>(emptyList())

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }

        // Live search observer
        viewModelScope.launch {
            _foodSearchQuery.collect { query ->
                if (query.isBlank()) {
                    _catalogSearchResults.value = emptyList()
                } else {
                    repository.searchFood(query.trim()).collect { results ->
                        _catalogSearchResults.value = results
                        val exactMatch = results.find { it.name.equals(query.trim(), ignoreCase = true) }
                        if (exactMatch != null && _selectedFoodItem.value == null) {
                            _selectedFoodItem.value = exactMatch
                        }
                    }
                }
            }
        }

        // Auto-update weight if sync is active
        viewModelScope.launch {
            repository.latestWeightLog.collect { latest ->
                if (latest != null && _calcAutoSyncWeight.value) {
                    _calcWeightKg.value = String.format(java.util.Locale.US, "%.1f", latest.weightKg)
                }
            }
        }
    }

    private val userConfigFlow = repository.userConfig
    private val todayLogsFlow = repository.getLogsByDate(todayString)
    private val weekLogsFlow = repository.getLogsForWeek(mondayDate, sundayDate)
    private val latestWeightFlow = repository.latestWeightLog

    private val databaseDataFlow = combine(
        userConfigFlow,
        todayLogsFlow,
        weekLogsFlow,
        latestWeightFlow
    ) { config, todayLogs, weekLogs, latestWeight ->
        val actualConfig = config ?: UserNutritionConfig()
        // If config changed autoSyncWeight, sync our local toggle
        _calcAutoSyncWeight.value = actualConfig.autoSyncWeight
        NutritionDatabaseData(
            config = actualConfig,
            todayLogs = todayLogs,
            weekLogs = weekLogs,
            latestWeight = latestWeight
        )
    }

    private val formInputsFlow = combine(
        _activeSheet,
        _foodSearchQuery,
        _selectedFoodItem,
        _gramsInput,
        _catalogSearchResults
    ) { sheet, query, selectedFood, grams, searchResults ->
        NutritionFormInputs(
            activeSheet = sheet,
            foodSearchQuery = query,
            selectedFoodItem = selectedFood,
            gramsInput = grams,
            catalogSearchResults = searchResults
        )
    }

    val uiState: StateFlow<NutritionUiState> = combine(
        databaseDataFlow,
        formInputsFlow
    ) { dbData, formInputs ->

        val actualConfig = dbData.config
        val dailyTarget = actualConfig.dailyCalorieTarget

        val todayCal = dbData.todayLogs.sumOf { it.calories }
        val todayProt = dbData.todayLogs.fold(0f) { acc, log -> acc + log.proteinGrams }
        val todayCarb = dbData.todayLogs.fold(0f) { acc, log -> acc + log.carbsGrams }
        val todayFat = dbData.todayLogs.fold(0f) { acc, log -> acc + log.fatGrams }

        val remaining = dailyTarget - todayCal
        val progress = if (dailyTarget > 0) ((todayCal.toFloat() / dailyTarget) * 100).roundToInt() else 0

        // Week aggregation
        val weekTotal = dbData.weekLogs.sumOf { it.calories }
        val weekTarget = dailyTarget * 7

        val daysWithCalories = dbData.weekLogs.groupBy { it.dateString }
            .filter { entry -> entry.value.sumOf { it.calories } > 0 }
            .size
        val weekAvg = if (daysWithCalories > 0) weekTotal / daysWithCalories else 0

        // Build 7-day data (Monday to Sunday)
        val dayLabels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val monday = today.with(DayOfWeek.MONDAY)
        val weeklyDays = (0..6).map { dayOffset ->
            val d = monday.plusDays(dayOffset.toLong())
            val dStr = d.format(dateFormatter)
            val dayCals = dbData.weekLogs.filter { it.dateString == dStr }.sumOf { it.calories }
            val hasLogs = dayCals > 0
            WeeklyDayData(
                dayLabel = dayLabels[dayOffset],
                dateString = dStr,
                calories = dayCals,
                isToday = d == today,
                hasLogs = hasLogs
            )
        }

        val latestLoggedWeightVal = dbData.latestWeight?.weightKg ?: actualConfig.weightKg

        // Preview calculations for calculator
        val (calcBmr, calcTdee, calcTarget) = computeMifflinValues(
            sex = _calcSex.value,
            weightStr = _calcWeightKg.value,
            heightStr = _calcHeightCm.value,
            ageStr = _calcAge.value,
            activity = _calcActivityLevel.value,
            goal = _calcGoalType.value,
            adjustmentStr = _calcAdjustmentCalories.value
        )

        NutritionUiState(
            config = actualConfig,
            todayLogs = dbData.todayLogs,
            todayCalories = todayCal,
            todayProteinGrams = todayProt,
            todayCarbsGrams = todayCarb,
            todayFatGrams = todayFat,
            remainingCalories = remaining,
            progressPercent = progress,
            weekTotalCalories = weekTotal,
            weekTargetCalories = weekTarget,
            weekAverageCalories = weekAvg,
            weeklyDays = weeklyDays,
            activeSheet = formInputs.activeSheet,
            foodSearchQuery = formInputs.foodSearchQuery,
            catalogSearchResults = formInputs.catalogSearchResults,
            selectedFoodItem = formInputs.selectedFoodItem,
            gramsInput = formInputs.gramsInput,
            manualCaloriesPer100g = _manualCaloriesPer100g.value,
            manualProteinPer100g = _manualProteinPer100g.value,
            manualCarbsPer100g = _manualCarbsPer100g.value,
            manualFatPer100g = _manualFatPer100g.value,
            calcAutoSyncWeight = _calcAutoSyncWeight.value,
            latestLoggedWeight = latestLoggedWeightVal,
            calcSex = _calcSex.value,
            calcWeightKg = _calcWeightKg.value,
            calcHeightCm = _calcHeightCm.value,
            calcAge = _calcAge.value,
            calcActivityLevel = _calcActivityLevel.value,
            calcGoalType = _calcGoalType.value,
            calcAdjustmentCalories = _calcAdjustmentCalories.value,
            previewBmr = calcBmr,
            previewTdee = calcTdee,
            previewTarget = calcTarget
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NutritionUiState()
    )

    fun openSheet(sheet: ActiveBottomSheet) {
        _activeSheet.value = sheet
        if (sheet == ActiveBottomSheet.DAILY_INTAKE) {
            _foodSearchQuery.value = ""
            _selectedFoodItem.value = null
            _gramsInput.value = "120"
            _manualCaloriesPer100g.value = ""
            _manualProteinPer100g.value = ""
            _manualCarbsPer100g.value = ""
            _manualFatPer100g.value = ""
        } else if (sheet == ActiveBottomSheet.CALORIE_CALCULATOR) {
            val cur = uiState.value.config
            _calcAutoSyncWeight.value = cur.autoSyncWeight
            _calcSex.value = cur.sex
            val weightVal = if (cur.autoSyncWeight) {
                uiState.value.latestLoggedWeight
            } else {
                cur.weightKg
            }
            _calcWeightKg.value = String.format(java.util.Locale.US, "%.1f", weightVal)
            _calcHeightCm.value = cur.heightCm.toString()
            _calcAge.value = cur.age.toString()
            _calcActivityLevel.value = cur.activityLevel
            _calcGoalType.value = cur.goalType
            _calcAdjustmentCalories.value = cur.calorieAdjustment.toString()
        }
    }

    fun closeSheet() {
        _activeSheet.value = ActiveBottomSheet.NONE
    }

    fun updateSearchQuery(query: String) {
        _foodSearchQuery.value = query
        if (_selectedFoodItem.value != null && !_selectedFoodItem.value!!.name.equals(query, ignoreCase = true)) {
            _selectedFoodItem.value = null
        }
    }

    fun selectFood(food: FoodItem) {
        _selectedFoodItem.value = food
        _foodSearchQuery.value = food.name
    }

    fun clearSelectedFood() {
        _selectedFoodItem.value = null
    }

    fun updateGrams(grams: String) {
        _gramsInput.value = grams.filter { it.isDigit() }
    }

    fun updateManualValues(cal: String, prot: String, carbs: String, fat: String) {
        _manualCaloriesPer100g.value = cal
        _manualProteinPer100g.value = prot
        _manualCarbsPer100g.value = carbs
        _manualFatPer100g.value = fat
    }

    fun registerFoodIntake() {
        val grams = _gramsInput.value.toFloatOrNull() ?: 100f
        val selected = _selectedFoodItem.value

        viewModelScope.launch {
            if (selected != null) {
                val factor = grams / 100f
                val cal = (selected.caloriesPer100g * factor).roundToInt()
                val prot = selected.proteinPer100g * factor
                val carbs = selected.carbsPer100g * factor
                val fat = selected.fatPer100g * factor

                repository.insertLog(
                    DailyFoodLog(
                        foodName = selected.name,
                        gramsConsumed = grams,
                        calories = cal,
                        proteinGrams = prot,
                        carbsGrams = carbs,
                        fatGrams = fat,
                        timestampMillis = System.currentTimeMillis(),
                        dateString = todayString
                    )
                )
            } else {
                val foodName = _foodSearchQuery.value.trim().ifBlank { "Alimento personalizado" }
                val cal100 = _manualCaloriesPer100g.value.toFloatOrNull() ?: 0f
                val prot100 = _manualProteinPer100g.value.toFloatOrNull() ?: 0f
                val carbs100 = _manualCarbsPer100g.value.toFloatOrNull() ?: 0f
                val fat100 = _manualFatPer100g.value.toFloatOrNull() ?: 0f

                val newFood = FoodItem(
                    name = foodName,
                    caloriesPer100g = cal100,
                    proteinPer100g = prot100,
                    carbsPer100g = carbs100,
                    fatPer100g = fat100
                )
                repository.insertFood(newFood)

                val factor = grams / 100f
                val cal = (cal100 * factor).roundToInt()
                val prot = prot100 * factor
                val carbs = carbs100 * factor
                val fat = fat100 * factor

                repository.insertLog(
                    DailyFoodLog(
                        foodName = foodName,
                        gramsConsumed = grams,
                        calories = cal,
                        proteinGrams = prot,
                        carbsGrams = carbs,
                        fatGrams = fat,
                        timestampMillis = System.currentTimeMillis(),
                        dateString = todayString
                    )
                )
            }

            _foodSearchQuery.value = ""
            _selectedFoodItem.value = null
            _manualCaloriesPer100g.value = ""
            _manualProteinPer100g.value = ""
            _manualCarbsPer100g.value = ""
            _manualFatPer100g.value = ""
        }
    }

    fun deleteFoodLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLogById(id)
        }
    }

    // --- Calculator Events ---
    fun setCalcAutoSyncWeight(enabled: Boolean) {
        _calcAutoSyncWeight.value = enabled
        if (enabled) {
            _calcWeightKg.value = String.format(java.util.Locale.US, "%.1f", uiState.value.latestLoggedWeight)
        }
        viewModelScope.launch {
            repository.updateAutoSyncWeight(enabled)
        }
    }

    fun setCalcSex(sex: String) { _calcSex.value = sex }
    fun setCalcWeight(weight: String) {
        if (!_calcAutoSyncWeight.value) {
            _calcWeightKg.value = weight
        }
    }
    fun setCalcHeight(height: String) { _calcHeightCm.value = height }
    fun setCalcAge(age: String) { _calcAge.value = age }
    fun setCalcActivity(activity: String) { _calcActivityLevel.value = activity }
    fun setCalcGoal(goal: String) { _calcGoalType.value = goal }
    fun setCalcAdjustment(adjustment: String) { _calcAdjustmentCalories.value = adjustment }

    fun applyCalculatedGoal() {
        val (bmr, tdee, target) = computeMifflinValues(
            sex = _calcSex.value,
            weightStr = _calcWeightKg.value,
            heightStr = _calcHeightCm.value,
            ageStr = _calcAge.value,
            activity = _calcActivityLevel.value,
            goal = _calcGoalType.value,
            adjustmentStr = _calcAdjustmentCalories.value
        )

        val updated = UserNutritionConfig(
            id = 1,
            sex = _calcSex.value,
            weightKg = _calcWeightKg.value.replace(',', '.').toFloatOrNull() ?: 74.5f,
            targetWeightKg = uiState.value.config.targetWeightKg,
            autoSyncWeight = _calcAutoSyncWeight.value,
            heightCm = _calcHeightCm.value.toIntOrNull() ?: 178,
            age = _calcAge.value.toIntOrNull() ?: 26,
            activityLevel = _calcActivityLevel.value,
            goalType = _calcGoalType.value,
            calorieAdjustment = _calcAdjustmentCalories.value.toIntOrNull() ?: 350,
            bmr = bmr,
            tdee = tdee,
            dailyCalorieTarget = target
        )

        viewModelScope.launch {
            repository.saveUserConfig(updated)
            closeSheet()
        }
    }

    companion object {
        fun computeMifflinValues(
            sex: String,
            weightStr: String,
            heightStr: String,
            ageStr: String,
            activity: String,
            goal: String,
            adjustmentStr: String
        ): Triple<Int, Int, Int> {
            val weight = weightStr.replace(',', '.').toFloatOrNull() ?: 74.5f
            val height = heightStr.toIntOrNull() ?: 178
            val age = ageStr.toIntOrNull() ?: 26
            val adjustment = adjustmentStr.toIntOrNull() ?: 350

            val base = (10f * weight) + (6.25f * height) - (5f * age)
            val bmr = if (sex == "FEMALE") (base - 161f).roundToInt() else (base + 5f).roundToInt()

            val factor = when (activity) {
                "SEDENTARY" -> 1.200f
                "LIGHT" -> 1.375f
                "MODERATE" -> 1.550f
                "HIGH" -> 1.725f
                else -> 1.550f
            }

            val tdee = (bmr * factor).roundToInt()

            val target = when (goal) {
                "DEFICIT" -> maxOf(500, tdee - adjustment)
                "SURPLUS" -> tdee + adjustment
                "MAINTENANCE" -> tdee
                else -> tdee
            }

            return Triple(bmr, tdee, target)
        }
    }
}
