package com.poorrat.gotcha.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poorrat.gotcha.ui.nutrition.components.PoorRatBottomBar
import com.poorrat.gotcha.ui.workout.components.MainEntrenaCard
import com.poorrat.gotcha.ui.workout.components.PoorRatWorkoutTopBar
import com.poorrat.gotcha.ui.workout.components.WorkoutDayHeader
import com.poorrat.gotcha.ui.workout.components.WorkoutNavCards
import com.poorrat.gotcha.ui.workout.modals.ProgressSessionBottomSheet
import com.poorrat.gotcha.ui.workout.modals.RoutineDetailBottomSheet
import com.poorrat.gotcha.ui.workout.modals.ScheduleRoutineBottomSheet
import com.poorrat.gotcha.ui.theme.PoorRatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onNavigateToNutrition: () -> Unit,
    onNavigateToWeight: () -> Unit,
    onNavigateToSupplements: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onToggleTheme: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = PoorRatTheme.colors

    val progressSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isRestDay = uiState.todayRoutineDay?.bodyPart == "Descanso"

    Scaffold(
        topBar = {
            PoorRatWorkoutTopBar(onToggleTheme = onToggleTheme)
        },
        bottomBar = {
            PoorRatBottomBar(
                currentTab = "Entreno",
                onTabSelected = { tab ->
                    when (tab) {
                        "Alimentación" -> onNavigateToNutrition()
                        "Peso" -> onNavigateToWeight()
                        "Suplementos" -> onNavigateToSupplements()
                        "Tienda" -> onNavigateToShop()
                    }
                }
            )
        },
        containerColor = colors.background,
        modifier = modifier.fillMaxSize().testTag("workout_main_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Cabecera del Día Actual
            WorkoutDayHeader(dayEntity = uiState.todayRoutineDay)

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Principal ENTRENA
            MainEntrenaCard(
                isRestDay = isRestDay,
                exerciseCount = uiState.todayExercises.size,
                isSessionRunning = uiState.isSessionRunning,
                onClick = { viewModel.startOrResumeSession() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tarjetas de Navegación a Submódulos
            WorkoutNavCards(
                exerciseCount = uiState.todayExercises.size,
                todayExercises = uiState.todayExercises,
                onViewRoutineClick = { viewModel.openSheet(WorkoutActiveSheet.ROUTINE_DETAIL) },
                onProgressClick = { viewModel.startOrResumeSession() },
                onScheduleRoutineClick = { viewModel.openSheet(WorkoutActiveSheet.SCHEDULE_ROUTINE) }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Sub-vista 1: PROGRESO — Sesión de Entrenamiento Activa
        if (uiState.activeSheet == WorkoutActiveSheet.PROGRESS_ACTIVE_SESSION) {
            ProgressSessionBottomSheet(
                uiState = uiState,
                sheetState = progressSheetState,
                onDismiss = { viewModel.closeSheet() },
                onExerciseCompletedChange = { id, comp -> viewModel.updateExerciseCompleted(id, comp) },
                onExerciseWeightChange = { id, w -> viewModel.updateExerciseAchievedWeight(id, w) },
                onExerciseRepsChange = { id, r -> viewModel.updateExerciseAchievedReps(id, r) },
                onFinishSession = { viewModel.finishAndSaveSession() }
            )
        }

        // Sub-vista 2: Detalle de Rutina
        if (uiState.activeSheet == WorkoutActiveSheet.ROUTINE_DETAIL) {
            RoutineDetailBottomSheet(
                uiState = uiState,
                sheetState = detailSheetState,
                onDismiss = { viewModel.closeSheet() },
                onSelectDay = { viewModel.selectDetailDay(it) }
            )
        }

        // Sub-vista 3: Programar Rutina
        if (uiState.activeSheet == WorkoutActiveSheet.SCHEDULE_ROUTINE) {
            ScheduleRoutineBottomSheet(
                uiState = uiState,
                sheetState = scheduleSheetState,
                onDismiss = { viewModel.closeSheet() },
                onSelectDay = { viewModel.loadDayToSchedule(it) },
                onRoutineNameChange = { viewModel.updateScheduleRoutineName(it) },
                onBodyPartChange = { viewModel.updateScheduleBodyPart(it) },
                onNewExNameChange = { viewModel.updateNewExName(it) },
                onNewExSetsChange = { viewModel.updateNewExSets(it) },
                onNewExRepsChange = { viewModel.updateNewExReps(it) },
                onNewExWeightChange = { viewModel.updateNewExWeight(it) },
                onAddExercise = { viewModel.addExerciseToSchedule() },
                onRemoveExercise = { viewModel.removeExerciseFromSchedule(it) },
                onSaveSchedule = { viewModel.saveSchedule() }
            )
        }
    }
}
