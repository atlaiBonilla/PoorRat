package com.poorrat.gotcha.ui.workout

import com.poorrat.gotcha.data.local.entity.RoutineDayEntity
import com.poorrat.gotcha.data.local.entity.RoutineExerciseEntity

enum class WorkoutActiveSheet {
    NONE,
    PROGRESS_ACTIVE_SESSION,
    ROUTINE_DETAIL,
    SCHEDULE_ROUTINE
}

data class ExerciseSessionInput(
    val isCompleted: Boolean = false,
    val achievedWeightText: String = "",
    val achievedRepsText: String = ""
)

data class WorkoutUiState(
    val currentDayOfWeek: Int = 1,
    val todayRoutineDay: RoutineDayEntity? = null,
    val todayExercises: List<RoutineExerciseEntity> = emptyList(),
    val allRoutineDays: List<RoutineDayEntity> = emptyList(),
    val allExercises: List<RoutineExerciseEntity> = emptyList(),
    val activeSheet: WorkoutActiveSheet = WorkoutActiveSheet.NONE,

    // Active session state (Modal 1)
    val isSessionRunning: Boolean = false,
    val sessionElapsedSeconds: Long = 0L,
    val sessionExerciseInputs: Map<Long, ExerciseSessionInput> = emptyMap(),

    // Detail modal state (Modal 2)
    val detailSelectedDayOfWeek: Int = 1,
    val detailRoutineDay: RoutineDayEntity? = null,
    val detailExercises: List<RoutineExerciseEntity> = emptyList(),

    // Schedule modal state (Modal 3)
    val scheduleSelectedDayOfWeek: Int = 1,
    val scheduleRoutineName: String = "",
    val scheduleBodyPart: String = "Upper",
    val scheduleExercises: List<RoutineExerciseEntity> = emptyList(),
    val newExerciseName: String = "",
    val newExerciseSets: String = "4",
    val newExerciseReps: String = "8-10",
    val newExerciseWeight: String = "85"
)
