package com.poorrat.gotchadev.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poorrat.gotchadev.data.ai.EstimatedFoodResult
import com.poorrat.gotchadev.data.ai.FoodSearchGroundingEstimator
import com.poorrat.gotchadev.data.ai.GroundingResult
import com.poorrat.gotchadev.data.local.entity.DailyFoodLog
import com.poorrat.gotchadev.data.local.entity.FoodItem
import com.poorrat.gotchadev.data.local.entity.UserNutritionConfig
import com.poorrat.gotchadev.data.local.entity.WeightLog
import com.poorrat.gotchadev.data.repository.NutritionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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
    val activeAddFoodTab: Int,
    val aiFoodPrompt: String,
    val isAiEstimating: Boolean,
    val aiEstimatedResult: EstimatedFoodResult?,
    val aiErrorMessage: String?,
    val manualFoodName: String,
    val manualGrams: String,
    val manualCaloriesTotal: String,
    val manualProteinTotal: String,
    val manualCarbsTotal: String,
    val manualFatTotal: String,
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

    // Add Food Tabs: 0 = Catálogo (primera opción por defecto), 1 = IA, 2 = Manual
    private val _activeAddFoodTab = MutableStateFlow(0)

    // AI Estimation state
    private val _aiFoodPrompt = MutableStateFlow("")
    private val _isAiEstimating = MutableStateFlow(false)
    private val _aiEstimatedResult = MutableStateFlow<EstimatedFoodResult?>(null)
    private val _aiErrorMessage = MutableStateFlow<String?>(null)

    // Manual Food Entry state
    private val _manualFoodName = MutableStateFlow("")
    private val _manualGrams = MutableStateFlow("")
    private val _manualCaloriesTotal = MutableStateFlow("")
    private val _manualProteinTotal = MutableStateFlow("")
    private val _manualCarbsTotal = MutableStateFlow("")
    private val _manualFatTotal = MutableStateFlow("")

    // Catalog state
    private val _foodSearchQuery = MutableStateFlow("")
    private val _selectedFoodItem = MutableStateFlow<FoodItem?>(null)
    private val _gramsInput = MutableStateFlow("120")
    private val _manualCaloriesPer100g = MutableStateFlow("")
    private val _manualProteinPer100g = MutableStateFlow("")
    private val _manualCarbsPer100g = MutableStateFlow("")
    private val _manualFatPer100g = MutableStateFlow("")
    private val _catalogSearchResults = MutableStateFlow<List<FoodItem>>(emptyList())

    // Calculator inputs (all numeric defaults empty string per requirement)
    private val _calcAutoSyncWeight = MutableStateFlow(true)
    private val _calcSex = MutableStateFlow("MALE")
    private val _calcWeightKg = MutableStateFlow("")
    private val _calcHeightCm = MutableStateFlow("")
    private val _calcAge = MutableStateFlow("")
    private val _calcActivityLevel = MutableStateFlow("MODERATE")
    private val _calcGoalType = MutableStateFlow("DEFICIT")
    private val _calcAdjustmentCalories = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }

        // High-performance search observer for Catalog tab
        // Runs on Dispatchers.Default with debounce so typing in the text field never drops frames or input events
        viewModelScope.launch(Dispatchers.Default) {
            combine(repository.getAllFoods(), _foodSearchQuery) { allFoods, rawQuery ->
                Pair(allFoods, rawQuery)
            }
                .flowOn(Dispatchers.Default)
                .collectLatest { (allFoods, rawQuery) ->
                    val query = rawQuery.trim()
                    if (query.isNotBlank()) {
                        delay(60L)
                    }
                    val filtered = if (query.isBlank()) {
                        allFoods
                    } else {
                        val normalizedTokens = normalizeForSearch(query)
                            .split(Regex("\\s+"))
                            .filter { it.isNotBlank() }
                        allFoods.filter { item ->
                            val normalizedName = normalizeForSearch(item.name)
                            normalizedTokens.all { token -> normalizedName.contains(token) }
                        }
                    }
                    _catalogSearchResults.value = filtered
                }
        }

        // Auto-update weight if sync is active
        viewModelScope.launch {
            repository.latestWeightLog.collect { latest ->
                if (latest != null && latest.weightKg > 0f && _calcAutoSyncWeight.value) {
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
        _calcAutoSyncWeight.value = actualConfig.autoSyncWeight
        NutritionDatabaseData(
            config = actualConfig,
            todayLogs = todayLogs,
            weekLogs = weekLogs,
            latestWeight = latestWeight
        )
    }

    private data class AiInputs(
        val tab: Int,
        val prompt: String,
        val estimating: Boolean,
        val result: EstimatedFoodResult?,
        val error: String?
    )

    private data class ManualInputs(
        val name: String,
        val grams: String,
        val calories: String,
        val protein: String,
        val carbs: String,
        val fat: String
    )

    private data class CatalogInputs(
        val query: String,
        val selectedItem: FoodItem?,
        val grams: String,
        val searchResults: List<FoodItem>
    )

    private val aiInputsFlow = combine(
        _activeAddFoodTab,
        _aiFoodPrompt,
        _isAiEstimating,
        _aiEstimatedResult,
        _aiErrorMessage
    ) { tab, prompt, estimating, result, error ->
        AiInputs(tab, prompt, estimating, result, error)
    }

    private val manualMacrosFlow = combine(
        _manualCaloriesTotal,
        _manualProteinTotal,
        _manualCarbsTotal,
        _manualFatTotal
    ) { cal, prot, carb, fat ->
        listOf(cal, prot, carb, fat)
    }

    private val manualInputsFlow = combine(
        _manualFoodName,
        _manualGrams,
        manualMacrosFlow
    ) { name, g, macros ->
        ManualInputs(
            name = name,
            grams = g,
            calories = macros[0],
            protein = macros[1],
            carbs = macros[2],
            fat = macros[3]
        )
    }

    private val catalogInputsFlow = combine(
        _foodSearchQuery,
        _selectedFoodItem,
        _gramsInput,
        _catalogSearchResults
    ) { query, selected, grams, results ->
        CatalogInputs(query, selected, grams, results)
    }

    private val formInputsFlow = combine(
        _activeSheet,
        aiInputsFlow,
        manualInputsFlow,
        catalogInputsFlow
    ) { sheet, ai, manual, catalog ->
        NutritionFormInputs(
            activeSheet = sheet,
            activeAddFoodTab = ai.tab,
            aiFoodPrompt = ai.prompt,
            isAiEstimating = ai.estimating,
            aiEstimatedResult = ai.result,
            aiErrorMessage = ai.error,
            manualFoodName = manual.name,
            manualGrams = manual.grams,
            manualCaloriesTotal = manual.calories,
            manualProteinTotal = manual.protein,
            manualCarbsTotal = manual.carbs,
            manualFatTotal = manual.fat,
            foodSearchQuery = catalog.query,
            selectedFoodItem = catalog.selectedItem,
            gramsInput = catalog.grams,
            catalogSearchResults = catalog.searchResults
        )
    }

    private data class CalcPhysicalInputs(
        val sex: String,
        val weightKg: String,
        val heightCm: String,
        val age: String
    )

    private data class CalcGoalInputs(
        val activityLevel: String,
        val goalType: String,
        val adjustmentCalories: String,
        val autoSyncWeight: Boolean
    )

    private data class CalculatorInputs(
        val sex: String,
        val weightKg: String,
        val heightCm: String,
        val age: String,
        val activityLevel: String,
        val goalType: String,
        val adjustmentCalories: String,
        val autoSyncWeight: Boolean
    )

    private val calcPhysicalFlow = combine(
        _calcSex,
        _calcWeightKg,
        _calcHeightCm,
        _calcAge
    ) { sex, weight, height, age ->
        CalcPhysicalInputs(sex, weight, height, age)
    }

    private val calcGoalFlow = combine(
        _calcActivityLevel,
        _calcGoalType,
        _calcAdjustmentCalories,
        _calcAutoSyncWeight
    ) { activity, goal, adjustment, autoSync ->
        CalcGoalInputs(activity, goal, adjustment, autoSync)
    }

    private val calculatorInputsFlow = combine(
        calcPhysicalFlow,
        calcGoalFlow
    ) { physical, goal ->
        CalculatorInputs(
            sex = physical.sex,
            weightKg = physical.weightKg,
            heightCm = physical.heightCm,
            age = physical.age,
            activityLevel = goal.activityLevel,
            goalType = goal.goalType,
            adjustmentCalories = goal.adjustmentCalories,
            autoSyncWeight = goal.autoSyncWeight
        )
    }

    val uiState: StateFlow<NutritionUiState> = combine(
        databaseDataFlow,
        formInputsFlow,
        calculatorInputsFlow
    ) { dbData, formInputs, calcInputs ->

        val actualConfig = dbData.config
        val dailyTarget = actualConfig.dailyCalorieTarget

        val todayCal = dbData.todayLogs.sumOf { it.calories }
        val todayProt = dbData.todayLogs.fold(0f) { acc, log -> acc + log.proteinGrams }
        val todayCarb = dbData.todayLogs.fold(0f) { acc, log -> acc + log.carbsGrams }
        val todayFat = dbData.todayLogs.fold(0f) { acc, log -> acc + log.fatGrams }

        val remaining = if (dailyTarget > 0) dailyTarget - todayCal else 0
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
            sex = calcInputs.sex,
            weightStr = calcInputs.weightKg,
            heightStr = calcInputs.heightCm,
            ageStr = calcInputs.age,
            activity = calcInputs.activityLevel,
            goal = calcInputs.goalType,
            adjustmentStr = calcInputs.adjustmentCalories
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
            activeAddFoodTab = formInputs.activeAddFoodTab,
            aiFoodPrompt = formInputs.aiFoodPrompt,
            isAiEstimating = formInputs.isAiEstimating,
            aiEstimatedResult = formInputs.aiEstimatedResult,
            aiErrorMessage = formInputs.aiErrorMessage,
            manualFoodName = formInputs.manualFoodName,
            manualGrams = formInputs.manualGrams,
            manualCaloriesTotal = formInputs.manualCaloriesTotal,
            manualProteinTotal = formInputs.manualProteinTotal,
            manualCarbsTotal = formInputs.manualCarbsTotal,
            manualFatTotal = formInputs.manualFatTotal,
            foodSearchQuery = formInputs.foodSearchQuery,
            catalogSearchResults = formInputs.catalogSearchResults,
            selectedFoodItem = formInputs.selectedFoodItem,
            gramsInput = formInputs.gramsInput,
            manualCaloriesPer100g = _manualCaloriesPer100g.value,
            manualProteinPer100g = _manualProteinPer100g.value,
            manualCarbsPer100g = _manualCarbsPer100g.value,
            manualFatPer100g = _manualFatPer100g.value,
            calcAutoSyncWeight = calcInputs.autoSyncWeight,
            latestLoggedWeight = latestLoggedWeightVal,
            calcSex = calcInputs.sex,
            calcWeightKg = calcInputs.weightKg,
            calcHeightCm = calcInputs.heightCm,
            calcAge = calcInputs.age,
            calcActivityLevel = calcInputs.activityLevel,
            calcGoalType = calcInputs.goalType,
            calcAdjustmentCalories = calcInputs.adjustmentCalories,
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
            _activeAddFoodTab.value = 0
            _aiFoodPrompt.value = ""
            _aiEstimatedResult.value = null
            _aiErrorMessage.value = null
            _manualFoodName.value = ""
            _manualGrams.value = ""
            _manualCaloriesTotal.value = ""
            _manualProteinTotal.value = ""
            _manualCarbsTotal.value = ""
            _manualFatTotal.value = ""
            _foodSearchQuery.value = ""
            _selectedFoodItem.value = null
            _gramsInput.value = "120"
        } else if (sheet == ActiveBottomSheet.CALORIE_CALCULATOR) {
            val cur = uiState.value.config
            _calcAutoSyncWeight.value = cur.autoSyncWeight
            _calcSex.value = cur.sex
            val weightVal = if (cur.autoSyncWeight) {
                uiState.value.latestLoggedWeight
            } else {
                cur.weightKg
            }
            _calcWeightKg.value = if (weightVal > 0f) String.format(java.util.Locale.US, "%.1f", weightVal) else ""
            _calcHeightCm.value = if (cur.heightCm > 0) cur.heightCm.toString() else ""
            _calcAge.value = if (cur.age > 0) cur.age.toString() else ""
            _calcActivityLevel.value = cur.activityLevel
            _calcGoalType.value = cur.goalType
            _calcAdjustmentCalories.value = if (cur.calorieAdjustment > 0) cur.calorieAdjustment.toString() else ""
        }
    }

    fun closeSheet() {
        _activeSheet.value = ActiveBottomSheet.NONE
    }

    // --- Add Food Tabs ---
    fun setAddFoodTab(tab: Int) {
        _activeAddFoodTab.value = tab
    }

    // --- AI Estimation ---
    fun updateAiFoodPrompt(prompt: String) {
        _aiFoodPrompt.value = prompt
    }

    fun estimateFoodWithAi() {
        val prompt = _aiFoodPrompt.value.trim()
        if (prompt.isBlank()) {
            _aiErrorMessage.value = "Ingresa lo que comiste para que la IA pueda calcularlo"
            _aiEstimatedResult.value = null
            return
        }

        viewModelScope.launch {
            _isAiEstimating.value = true
            _aiErrorMessage.value = null
            _aiEstimatedResult.value = null
            try {
                // Real macros grounded in Google Search via Gemini 3.8 Flash
                when (val result = FoodSearchGroundingEstimator.estimateFood(prompt)) {
                    is GroundingResult.Success -> {
                        _aiEstimatedResult.value = result.result
                        _aiErrorMessage.value = null
                    }
                    is GroundingResult.NotEdible -> {
                        _aiEstimatedResult.value = null
                        _aiErrorMessage.value = "No se identificó ningún alimento o bebida comestible en tu texto (ej. objetos, acciones o texto no relacionado). Prueba con alimentos reales, platos o combos."
                    }
                    is GroundingResult.Failure -> {
                        _aiEstimatedResult.value = null
                        val rawMsg = result.message
                        _aiErrorMessage.value = rawMsg
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiErrorMessage.value = "Error al estimar con Google Search: ${e.localizedMessage ?: "Verifica tu conexión"}"
                _aiEstimatedResult.value = null
            } finally {
                _isAiEstimating.value = false
            }
        }
    }

    fun updateEstimatedValues(
        name: String,
        grams: Float,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float
    ) {
        _aiEstimatedResult.value = _aiEstimatedResult.value?.copy(
            foodName = name,
            grams = grams,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat
        ) ?: EstimatedFoodResult(name, grams, calories, protein, carbs, fat)
    }

    fun registerAiEstimatedFood() {
        val result = _aiEstimatedResult.value ?: return
        viewModelScope.launch {
            repository.insertLog(
                DailyFoodLog(
                    foodName = result.foodName,
                    gramsConsumed = result.grams,
                    calories = result.calories,
                    proteinGrams = result.protein,
                    carbsGrams = result.carbs,
                    fatGrams = result.fat,
                    timestampMillis = System.currentTimeMillis(),
                    dateString = todayString
                )
            )
            _aiFoodPrompt.value = ""
            _aiEstimatedResult.value = null
            closeSheet()
        }
    }

    // --- Manual Food Entry ---
    fun updateManualFoodForm(
        name: String,
        grams: String,
        cal: String,
        prot: String,
        carbs: String,
        fat: String
    ) {
        _manualFoodName.value = name
        _manualGrams.value = grams.filter { it.isDigit() || it == '.' || it == ',' }
        _manualCaloriesTotal.value = cal.filter { it.isDigit() }
        _manualProteinTotal.value = prot.filter { it.isDigit() || it == '.' || it == ',' }
        _manualCarbsTotal.value = carbs.filter { it.isDigit() || it == '.' || it == ',' }
        _manualFatTotal.value = fat.filter { it.isDigit() || it == '.' || it == ',' }
    }

    fun registerManualFood() {
        val name = _manualFoodName.value.trim().ifBlank { "Comida personalizada" }
        val grams = _manualGrams.value.replace(',', '.').toFloatOrNull() ?: 100f
        // Any field left empty or unknown is taken as 0 per user requirement
        val cal = _manualCaloriesTotal.value.toIntOrNull() ?: 0
        val prot = _manualProteinTotal.value.replace(',', '.').toFloatOrNull() ?: 0f
        val carbs = _manualCarbsTotal.value.replace(',', '.').toFloatOrNull() ?: 0f
        val fat = _manualFatTotal.value.replace(',', '.').toFloatOrNull() ?: 0f

        viewModelScope.launch {
            repository.insertLog(
                DailyFoodLog(
                    foodName = name,
                    gramsConsumed = grams,
                    calories = cal,
                    proteinGrams = prot,
                    carbsGrams = carbs,
                    fatGrams = fat,
                    timestampMillis = System.currentTimeMillis(),
                    dateString = todayString
                )
            )
            _manualFoodName.value = ""
            _manualGrams.value = ""
            _manualCaloriesTotal.value = ""
            _manualProteinTotal.value = ""
            _manualCarbsTotal.value = ""
            _manualFatTotal.value = ""
            closeSheet()
        }
    }

    // --- Catalog Functions ---
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
        _foodSearchQuery.value = ""
    }

    fun useTypedQueryAsFood(name: String) {
        val trimmed = name.trim().ifBlank { "Alimento personalizado" }
        _selectedFoodItem.value = FoodItem(
            id = 0L,
            name = trimmed,
            caloriesPer100g = 150f,
            proteinPer100g = 10f,
            carbsPer100g = 15f,
            fatPer100g = 5f
        )
        _foodSearchQuery.value = trimmed
    }

    fun switchToAiWithQuery(query: String) {
        _aiFoodPrompt.value = query.trim()
        _activeAddFoodTab.value = 1
    }

    fun switchToManualWithQuery(query: String) {
        _manualFoodName.value = query.trim()
        _activeAddFoodTab.value = 2
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
            closeSheet()
        }
    }

    fun deleteFoodLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLogById(id)
        }
    }

    // --- Calculator Methods ---
    fun setCalcAutoSyncWeight(enabled: Boolean) {
        _calcAutoSyncWeight.value = enabled
        if (enabled) {
            val latest = uiState.value.latestLoggedWeight
            if (latest > 0f) {
                _calcWeightKg.value = String.format(java.util.Locale.US, "%.1f", latest)
            }
        }
        viewModelScope.launch {
            repository.updateAutoSyncWeight(enabled)
        }
    }

    fun setCalcSex(sex: String) { _calcSex.value = sex }
    fun setCalcWeight(weight: String) {
        if (!_calcAutoSyncWeight.value) {
            _calcWeightKg.value = weight.filter { it.isDigit() || it == '.' || it == ',' }
        }
    }
    fun setCalcHeight(height: String) { _calcHeightCm.value = height.filter { it.isDigit() } }
    fun setCalcAge(age: String) { _calcAge.value = age.filter { it.isDigit() } }
    fun setCalcActivity(activity: String) { _calcActivityLevel.value = activity }
    fun setCalcGoal(goal: String) { _calcGoalType.value = goal }
    fun setCalcAdjustment(adjustment: String) { _calcAdjustmentCalories.value = adjustment.filter { it.isDigit() } }

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
            weightKg = _calcWeightKg.value.replace(',', '.').toFloatOrNull() ?: 0f,
            targetWeightKg = uiState.value.config.targetWeightKg,
            autoSyncWeight = _calcAutoSyncWeight.value,
            heightCm = _calcHeightCm.value.toIntOrNull() ?: 0,
            age = _calcAge.value.toIntOrNull() ?: 0,
            activityLevel = _calcActivityLevel.value,
            goalType = _calcGoalType.value,
            calorieAdjustment = _calcAdjustmentCalories.value.toIntOrNull() ?: 0,
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
            val weight = weightStr.replace(',', '.').toFloatOrNull() ?: 0f
            val height = heightStr.toIntOrNull() ?: 0
            val age = ageStr.toIntOrNull() ?: 0
            val adjustment = adjustmentStr.toIntOrNull() ?: 0

            if (weight <= 0f || height <= 0 || age <= 0) {
                return Triple(0, 0, 0)
            }

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

private fun normalizeForSearch(input: String): String {
    return input.lowercase()
        .replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
        .replace('ü', 'u')
        .replace('ñ', 'n')
}

private data class NutritionTuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private data class NutritionTuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
