package com.poorrat.gotcha.ui.weight

import com.poorrat.gotcha.data.local.entity.WeightLog

enum class WeightActiveSheet {
    NONE,
    WEIGHT_HISTORY,
    EDIT_REGISTER_WEIGHT
}

data class WeightUiState(
    val currentWeightKg: Float = 74.5f,
    val targetWeightKg: Float = 70.0f,
    val differenceKg: Float = -4.5f,
    val weightLogs: List<WeightLog> = emptyList(),
    val activeSheet: WeightActiveSheet = WeightActiveSheet.NONE,

    // Form inputs for Modal 2 (Editar / Registrar Peso)
    val inputWeightKg: String = "74.5",
    val inputTargetWeightKg: String = "70.0",
    val inputNote: String = "En ayunas"
)
