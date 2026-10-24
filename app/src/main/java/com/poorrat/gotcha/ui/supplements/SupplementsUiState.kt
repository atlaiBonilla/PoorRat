package com.poorrat.gotcha.ui.supplements

import com.poorrat.gotcha.data.local.entity.SupplementEntity

data class SupplementsUiState(
    val supplements: List<SupplementEntity> = emptyList(),
    val todayDate: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val completionPercentage: Int = 0,
    val activeAlarmsCount: Int = 0,
    val isDailyStackModalVisible: Boolean = false,
    val isAddSupplementModalVisible: Boolean = false,
    val isAdjustAlarmsModalVisible: Boolean = false,
    // Add Supplement Form State
    val addName: String = "",
    val addDosage: String = "",
    val addIntakeMoment: String = "Con el desayuno",
    val addAlarmEnabled: Boolean = false,
    val addAlarmHour: Int = 8,
    val addAlarmMinute: Int = 0,
    val addErrorMessage: String? = null
) {
    val completionSubtitle: String
        get() = "$completedCount de $totalCount tomas completadas"

    val activeAlarmsBadgeText: String
        get() = "$activeAlarmsCount activas"

    val progressFraction: Float
        get() = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
}
