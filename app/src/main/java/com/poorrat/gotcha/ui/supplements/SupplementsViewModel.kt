package com.poorrat.gotcha.ui.supplements

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poorrat.gotcha.alarm.SupplementAlarmScheduler
import com.poorrat.gotcha.data.local.entity.SupplementEntity
import com.poorrat.gotcha.data.repository.SupplementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupplementsViewModel(
    private val repository: SupplementRepository,
    private val alarmScheduler: SupplementAlarmScheduler
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
            addName = modalState.addName,
            addDosage = modalState.addDosage,
            addIntakeMoment = modalState.addIntakeMoment,
            addAlarmEnabled = modalState.addAlarmEnabled,
            addAlarmHour = modalState.addAlarmHour,
            addAlarmMinute = modalState.addAlarmMinute,
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
        viewModelScope.launch {
            repository.updateTakenState(
                id = supplementId,
                isTaken = !currentTaken,
                todayDate = today
            )
        }
    }

    fun deleteSupplement(supplementId: Long) {
        viewModelScope.launch {
            alarmScheduler.cancel(supplementId)
            repository.deleteSupplement(supplementId)
        }
    }

    fun updateAlarmConfig(supplementId: Long, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateAlarmConfig(supplementId, enabled, hour, minute)
            val updated = SupplementEntity(
                id = supplementId,
                name = uiState.value.supplements.firstOrNull { it.id == supplementId }?.name ?: "",
                dosage = uiState.value.supplements.firstOrNull { it.id == supplementId }?.dosage ?: "",
                intakeMoment = uiState.value.supplements.firstOrNull { it.id == supplementId }?.intakeMoment ?: "",
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
            addName = "",
            addDosage = "",
            addIntakeMoment = "Con el desayuno",
            addAlarmEnabled = false,
            addAlarmHour = 8,
            addAlarmMinute = 0,
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

        viewModelScope.launch {
            val newSupplement = SupplementEntity(
                name = nameTrimmed,
                dosage = dosageTrimmed,
                intakeMoment = current.addIntakeMoment,
                alarmEnabled = current.addAlarmEnabled,
                alarmHour = current.addAlarmHour,
                alarmMinute = current.addAlarmMinute,
                lastTakenDate = null
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
        val addName: String = "",
        val addDosage: String = "",
        val addIntakeMoment: String = "Con el desayuno",
        val addAlarmEnabled: Boolean = false,
        val addAlarmHour: Int = 8,
        val addAlarmMinute: Int = 0,
        val addErrorMessage: String? = null
    )
}
