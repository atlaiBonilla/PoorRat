package com.poorrat.gotcha.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poorrat.gotcha.data.local.entity.UserNutritionConfig
import com.poorrat.gotcha.data.local.entity.WeightLog
import com.poorrat.gotcha.data.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

private data class WeightData(
    val config: UserNutritionConfig,
    val logs: List<WeightLog>
)

private data class WeightForm(
    val activeSheet: WeightActiveSheet,
    val inputWeightKg: String,
    val inputTargetWeightKg: String,
    val inputNote: String
)

class WeightViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _activeSheet = MutableStateFlow(WeightActiveSheet.NONE)
    private val _inputWeightKg = MutableStateFlow("74.5")
    private val _inputTargetWeightKg = MutableStateFlow("70.0")
    private val _inputNote = MutableStateFlow("En ayunas")

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    private val dataFlow = combine(
        repository.userConfig,
        repository.allWeightLogs
    ) { config, logs ->
        WeightData(
            config = config ?: UserNutritionConfig(),
            logs = logs
        )
    }

    private val formFlow = combine(
        _activeSheet,
        _inputWeightKg,
        _inputTargetWeightKg,
        _inputNote
    ) { sheet, weight, target, note ->
        WeightForm(sheet, weight, target, note)
    }

    val uiState: StateFlow<WeightUiState> = combine(
        dataFlow,
        formFlow
    ) { data, form ->
        val latestLog = data.logs.firstOrNull()
        val curWeight = latestLog?.weightKg ?: data.config.weightKg
        val targetWeight = data.config.targetWeightKg
        val diff = targetWeight - curWeight

        WeightUiState(
            currentWeightKg = curWeight,
            targetWeightKg = targetWeight,
            differenceKg = diff,
            weightLogs = data.logs,
            activeSheet = form.activeSheet,
            inputWeightKg = form.inputWeightKg,
            inputTargetWeightKg = form.inputTargetWeightKg,
            inputNote = form.inputNote
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeightUiState()
    )

    fun openSheet(sheet: WeightActiveSheet) {
        _activeSheet.value = sheet
        if (sheet == WeightActiveSheet.EDIT_REGISTER_WEIGHT) {
            val cur = uiState.value.currentWeightKg
            val target = uiState.value.targetWeightKg
            _inputWeightKg.value = String.format(Locale.US, "%.1f", cur)
            _inputTargetWeightKg.value = String.format(Locale.US, "%.1f", target)
            _inputNote.value = "En ayunas"
        }
    }

    fun closeSheet() {
        _activeSheet.value = WeightActiveSheet.NONE
    }

    fun updateInputWeight(weight: String) {
        _inputWeightKg.value = weight
    }

    fun updateInputTargetWeight(target: String) {
        _inputTargetWeightKg.value = target
    }

    fun updateInputNote(note: String) {
        _inputNote.value = note
    }

    fun saveWeightRecord() {
        val weight = _inputWeightKg.value.replace(',', '.').toFloatOrNull() ?: 74.5f
        val target = _inputTargetWeightKg.value.replace(',', '.').toFloatOrNull() ?: 70.0f
        val note = _inputNote.value.trim().ifBlank { "Pesaje general" }

        viewModelScope.launch {
            repository.addWeightLog(
                weightKg = weight,
                note = note,
                targetWeightKg = target
            )
            closeSheet()
        }
    }

    fun deleteWeightRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteWeightLog(id)
        }
    }
}
