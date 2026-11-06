package com.poorrat.gotchadev.ui.supplements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poorrat.gotchadev.alarm.SupplementAlarmScheduler
import com.poorrat.gotchadev.data.ai.FoodSearchGroundingEstimator
import com.poorrat.gotchadev.data.local.entity.DailyFoodLog
import com.poorrat.gotchadev.data.local.entity.SupplementEntity
import com.poorrat.gotchadev.data.repository.NutritionRepository
import com.poorrat.gotchadev.data.repository.SupplementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToInt

class SupplementsViewModel(
    private val repository: SupplementRepository,
    private val alarmScheduler: SupplementAlarmScheduler,
    private val nutritionRepository: NutritionRepository? = null
) : ViewModel() {

    private val _modalState = MutableStateFlow(
        ModalAndFormState(
            todayDate = getTodayDateString()
        )
    )

    val uiState: StateFlow<SupplementsUiState> = combine(
        repository.allSupplements,
        _modalState
    ) { supplements, modalState ->
        val today = modalState.todayDate
        val completedCount = supplements.count { it.lastTakenDate == today }
        val totalCount = supplements.size
        val completionPercentage = if (totalCount > 0) {
            (completedCount * 100) / totalCount
        } else 0
        val activeAlarmsCount = supplements.count { it.alarmEnabled }

        SupplementsUiState(
            supplements = supplements,
            todayDate = today,
            completedCount = completedCount,
            totalCount = totalCount,
            completionPercentage = completionPercentage,
            activeAlarmsCount = activeAlarmsCount,
            isDailyStackModalVisible = modalState.isDailyStackModalVisible,
            isAddSupplementModalVisible = modalState.isAddSupplementModalVisible,
            isAdjustAlarmsModalVisible = modalState.isAdjustAlarmsModalVisible,
            addTab = modalState.addTab,
            addAiPrompt = modalState.addAiPrompt,
            isAiEstimating = modalState.isAiEstimating,
            addName = modalState.addName,
            addDosage = modalState.addDosage,
            addIntakeMoment = modalState.addIntakeMoment,
            addAlarmEnabled = modalState.addAlarmEnabled,
            addAlarmHour = modalState.addAlarmHour,
            addAlarmMinute = modalState.addAlarmMinute,
            addHasCalories = modalState.addHasCalories,
            addIsDailyConsumption = modalState.addIsDailyConsumption,
            addCalories = modalState.addCalories,
            addProtein = modalState.addProtein,
            addCarbs = modalState.addCarbs,
            addFat = modalState.addFat,
            addErrorMessage = modalState.addErrorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SupplementsUiState(todayDate = getTodayDateString())
    )

    init {
        val today = getTodayDateString()
        viewModelScope.launch {
            repository.prepopulateIfEmpty(today)
        }
    }

    fun toggleTaken(supplementId: Long, currentTaken: Boolean) {
        val today = _modalState.value.todayDate
        val willBeTaken = !currentTaken
        viewModelScope.launch {
            repository.updateTakenState(
                id = supplementId,
                isTaken = willBeTaken,
                todayDate = today
            )

            val supplement = repository.getSupplementByIdSync(supplementId)
            if (supplement != null && supplement.hasCalories && supplement.isDailyConsumption) {
                val fullName = "${supplement.name} (${supplement.dosage})"
                if (willBeTaken) {
                    if (supplement.calories > 0) {
                        nutritionRepository?.insertLog(
                            DailyFoodLog(
                                foodName = fullName,
                                gramsConsumed = 100f,
                                calories = supplement.calories,
                                proteinGrams = supplement.protein,
                                carbsGrams = supplement.carbs,
                                fatGrams = supplement.fat,
                                timestampMillis = System.currentTimeMillis(),
                                dateString = today,
                                supplementId = supplementId
                            )
                        )
                    }
                } else {
                    // Desmarcado del consumo: remover del consumo diario
                    nutritionRepository?.removeSupplementLog(
                        dateString = today,
                        supplementId = supplementId,
                        foodName = fullName,
                        rawName = supplement.name
                    )
                }
            }
        }
    }

    fun deleteSupplement(supplementId: Long) {
        val today = _modalState.value.todayDate
        viewModelScope.launch {
            val supplement = repository.getSupplementByIdSync(supplementId)
            if (supplement != null) {
                val fullName = "${supplement.name} (${supplement.dosage})"
                nutritionRepository?.removeSupplementLog(
                    dateString = today,
                    supplementId = supplementId,
                    foodName = fullName,
                    rawName = supplement.name
                )
                nutritionRepository?.deleteLogsBySupplementId(supplementId)
            }
            alarmScheduler.cancel(supplementId)
            repository.deleteSupplement(supplementId)
        }
    }

    fun updateAlarmConfig(supplementId: Long, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateAlarmConfig(supplementId, enabled, hour, minute)
            val existing = repository.getSupplementByIdSync(supplementId)
            val updated = (existing ?: SupplementEntity(
                name = uiState.value.supplements.firstOrNull { it.id == supplementId }?.name ?: "",
                dosage = uiState.value.supplements.firstOrNull { it.id == supplementId }?.dosage ?: "",
                intakeMoment = uiState.value.supplements.firstOrNull { it.id == supplementId }?.intakeMoment ?: ""
            )).copy(
                id = supplementId,
                alarmEnabled = enabled,
                alarmHour = hour,
                alarmMinute = minute
            )
            if (enabled) {
                alarmScheduler.schedule(updated)
            } else {
                alarmScheduler.cancel(supplementId)
            }
        }
    }

    fun openDailyStackModal() {
        _modalState.value = _modalState.value.copy(isDailyStackModalVisible = true)
    }

    fun closeDailyStackModal() {
        _modalState.value = _modalState.value.copy(isDailyStackModalVisible = false)
    }

    fun openAddSupplementModal() {
        _modalState.value = _modalState.value.copy(
            isAddSupplementModalVisible = true,
            addTab = 0,
            addAiPrompt = "",
            isAiEstimating = false,
            addName = "",
            addDosage = "",
            addIntakeMoment = "Con el desayuno",
            addAlarmEnabled = false,
            addAlarmHour = 8,
            addAlarmMinute = 0,
            addHasCalories = false,
            addIsDailyConsumption = false,
            addCalories = "",
            addProtein = "",
            addCarbs = "",
            addFat = "",
            addErrorMessage = null
        )
    }

    fun closeAddSupplementModal() {
        _modalState.value = _modalState.value.copy(isAddSupplementModalVisible = false)
    }

    fun openAdjustAlarmsModal() {
        _modalState.value = _modalState.value.copy(isAdjustAlarmsModalVisible = true)
    }

    fun closeAdjustAlarmsModal() {
        _modalState.value = _modalState.value.copy(isAdjustAlarmsModalVisible = false)
    }

    fun onAddTabChanged(tabIndex: Int) {
        _modalState.value = _modalState.value.copy(addTab = tabIndex, addErrorMessage = null)
    }

    fun onAddAiPromptChanged(prompt: String) {
        _modalState.value = _modalState.value.copy(addAiPrompt = prompt, addErrorMessage = null)
    }

    fun onAddNameChanged(name: String) {
        _modalState.value = _modalState.value.copy(addName = name, addErrorMessage = null)
    }

    fun onAddDosageChanged(dosage: String) {
        _modalState.value = _modalState.value.copy(addDosage = dosage, addErrorMessage = null)
    }

    fun onAddIntakeMomentChanged(moment: String) {
        _modalState.value = _modalState.value.copy(addIntakeMoment = moment)
    }

    fun onAddAlarmEnabledChanged(enabled: Boolean) {
        _modalState.value = _modalState.value.copy(addAlarmEnabled = enabled)
    }

    fun onAddAlarmTimeChanged(hour: Int, minute: Int) {
        _modalState.value = _modalState.value.copy(addAlarmHour = hour, addAlarmMinute = minute)
    }

    fun onAddHasCaloriesChanged(hasCalories: Boolean) {
        _modalState.value = _modalState.value.copy(
            addHasCalories = hasCalories,
            addIsDailyConsumption = if (!hasCalories) false else _modalState.value.addIsDailyConsumption
        )
    }

    fun onAddIsDailyConsumptionChanged(isDaily: Boolean) {
        _modalState.value = _modalState.value.copy(addIsDailyConsumption = isDaily)
    }

    fun onAddCaloriesChanged(calories: String) {
        _modalState.value = _modalState.value.copy(addCalories = calories.filter { it.isDigit() })
    }

    fun onAddProteinChanged(protein: String) {
        _modalState.value = _modalState.value.copy(addProtein = protein.filter { it.isDigit() || it == '.' })
    }

    fun onAddCarbsChanged(carbs: String) {
        _modalState.value = _modalState.value.copy(addCarbs = carbs.filter { it.isDigit() || it == '.' })
    }

    fun onAddFatChanged(fat: String) {
        _modalState.value = _modalState.value.copy(addFat = fat.filter { it.isDigit() || it == '.' })
    }

    fun estimateSupplementWithAi() {
        val prompt = _modalState.value.addAiPrompt.trim()
        if (prompt.isBlank()) {
            _modalState.value = _modalState.value.copy(addErrorMessage = "Describe el suplemento o producto con sus datos")
            return
        }

        _modalState.value = _modalState.value.copy(isAiEstimating = true, addErrorMessage = null)

        viewModelScope.launch {
            val lower = prompt.lowercase(Locale.ROOT)

            // Check if user entered exact numbers in the text (e.g. 130 kcal, 24g proteina, etc.)
            val calMatch = Regex("""(\d+)\s*(?:kcal|calorias|calorías|cal)""").find(lower)
            val protMatch = Regex("""(\d+(?:\.\d+)?)\s*(?:g|gr|gramos)?\s*(?:de\s+)?(?:proteina|proteína|prot)""").find(lower)
                ?: Regex("""(?:proteina|proteína|prot)[:\s]+(\d+(?:\.\d+)?)""").find(lower)
            val carbsMatch = Regex("""(\d+(?:\.\d+)?)\s*(?:g|gr|gramos)?\s*(?:de\s+)?(?:carbohidratos|carbos|hidratos|ch)""").find(lower)
                ?: Regex("""(?:carbohidratos|carbos|ch)[:\s]+(\d+(?:\.\d+)?)""").find(lower)
            val fatMatch = Regex("""(\d+(?:\.\d+)?)\s*(?:g|gr|gramos)?\s*(?:de\s+)?(?:grasa|grasas|fat)""").find(lower)
                ?: Regex("""(?:grasa|grasas|fat)[:\s]+(\d+(?:\.\d+)?)""").find(lower)

            val parsedCal = calMatch?.groupValues?.get(1)?.toIntOrNull()
            val parsedProt = protMatch?.groupValues?.get(1)?.toFloatOrNull()
            val parsedCarbs = carbsMatch?.groupValues?.get(1)?.toFloatOrNull()
            val parsedFat = fatMatch?.groupValues?.get(1)?.toFloatOrNull()

            val aiResult = try {
                FoodSearchGroundingEstimator.estimateWithGoogleSearch(prompt)
            } catch (e: Exception) {
                null
            }

            if (parsedCal != null || parsedProt != null || aiResult != null) {
                val finalCal = parsedCal ?: aiResult?.calories ?: 0
                val finalProt = parsedProt ?: aiResult?.protein ?: 0f
                val finalCarbs = parsedCarbs ?: aiResult?.carbs ?: 0f
                val finalFat = parsedFat ?: aiResult?.fat ?: 0f

                val inferredName = aiResult?.foodName ?: prompt.split(",").firstOrNull()?.trim() ?: prompt
                val inferredDosage = if (aiResult != null && aiResult.grams > 0) {
                    if (aiResult.grams == 30f) "1 scoop (30g)" else "${aiResult.grams.roundToInt()}g"
                } else "1 porción"

                val hasCals = finalCal > 0 || finalProt > 0 || finalCarbs > 0 || finalFat > 0

                _modalState.value = _modalState.value.copy(
                    isAiEstimating = false,
                    addName = inferredName,
                    addDosage = inferredDosage,
                    addHasCalories = hasCals,
                    addIsDailyConsumption = hasCals,
                    addCalories = if (finalCal > 0) finalCal.toString() else "0",
                    addProtein = if (finalProt > 0) String.format(Locale.US, "%.1f", finalProt) else "0",
                    addCarbs = if (finalCarbs > 0) String.format(Locale.US, "%.1f", finalCarbs) else "0",
                    addFat = if (finalFat > 0) String.format(Locale.US, "%.1f", finalFat) else "0",
                    addErrorMessage = null
                )
            } else {
                _modalState.value = _modalState.value.copy(
                    isAiEstimating = false,
                    addErrorMessage = "No se detectó un suplemento o producto válido en Google Search. Prueba indicando dosis o valores nutricionales."
                )
            }
        }
    }

    fun submitAddSupplement() {
        val current = _modalState.value
        val nameTrimmed = current.addName.trim()
        val dosageTrimmed = current.addDosage.trim()

        if (nameTrimmed.isBlank()) {
            _modalState.value = current.copy(addErrorMessage = "Ingresa el nombre del suplemento")
            return
        }

        if (dosageTrimmed.isBlank()) {
            _modalState.value = current.copy(addErrorMessage = "Ingresa la dosis recomendada")
            return
        }

        val calInt = current.addCalories.toIntOrNull() ?: 0
        val protFloat = current.addProtein.toFloatOrNull() ?: 0f
        val carbsFloat = current.addCarbs.toFloatOrNull() ?: 0f
        val fatFloat = current.addFat.toFloatOrNull() ?: 0f

        viewModelScope.launch {
            val newSupplement = SupplementEntity(
                name = nameTrimmed,
                dosage = dosageTrimmed,
                intakeMoment = current.addIntakeMoment,
                alarmEnabled = current.addAlarmEnabled,
                alarmHour = current.addAlarmHour,
                alarmMinute = current.addAlarmMinute,
                lastTakenDate = null,
                hasCalories = current.addHasCalories,
                isDailyConsumption = current.addHasCalories && current.addIsDailyConsumption,
                calories = if (current.addHasCalories) calInt else 0,
                protein = if (current.addHasCalories) protFloat else 0f,
                carbs = if (current.addHasCalories) carbsFloat else 0f,
                fat = if (current.addHasCalories) fatFloat else 0f
            )
            val generatedId = repository.insertSupplement(newSupplement)
            if (current.addAlarmEnabled) {
                alarmScheduler.schedule(newSupplement.copy(id = generatedId))
            }
            closeAddSupplementModal()
        }
    }

    companion object {
        val INTAKE_MOMENTS = listOf(
            "Con el desayuno",
            "Con comida 1",
            "Con la cena",
            "30m antes de entrenar",
            "Post-entrenamiento"
        )

        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun formatTime12h(hour: Int, minute: Int): String {
            val period = if (hour < 12) "a. m." else "p. m."
            val hour12 = when (hour % 12) {
                0 -> 12
                else -> hour % 12
            }
            val hourStr = if (hour12 < 10) "0$hour12" else "$hour12"
            val minuteStr = if (minute < 10) "0$minute" else "$minute"
            return "$hourStr : $minuteStr   $period"
        }
    }

    private data class ModalAndFormState(
        val todayDate: String,
        val isDailyStackModalVisible: Boolean = false,
        val isAddSupplementModalVisible: Boolean = false,
        val isAdjustAlarmsModalVisible: Boolean = false,
        val addTab: Int = 0,
        val addAiPrompt: String = "",
        val isAiEstimating: Boolean = false,
        val addName: String = "",
        val addDosage: String = "",
        val addIntakeMoment: String = "Con el desayuno",
        val addAlarmEnabled: Boolean = false,
        val addAlarmHour: Int = 8,
        val addAlarmMinute: Int = 0,
        val addHasCalories: Boolean = false,
        val addIsDailyConsumption: Boolean = false,
        val addCalories: String = "",
        val addProtein: String = "",
        val addCarbs: String = "",
        val addFat: String = "",
        val addErrorMessage: String? = null
    )
}
