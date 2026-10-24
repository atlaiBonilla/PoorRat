package com.poorrat.gotcha.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poorrat.gotcha.ui.nutrition.components.DailyProgressAndMacrosCard
import com.poorrat.gotcha.ui.nutrition.components.ModuleNavCards
import com.poorrat.gotcha.ui.nutrition.components.PoorRatBottomBar
import com.poorrat.gotcha.ui.nutrition.components.PoorRatTopBar
import com.poorrat.gotcha.ui.nutrition.components.WeeklyVsDailySummaryCard
import com.poorrat.gotcha.ui.nutrition.modals.CalorieCalculatorBottomSheet
import com.poorrat.gotcha.ui.nutrition.modals.DailyIntakeBottomSheet
import com.poorrat.gotcha.ui.nutrition.modals.WeeklyIntakeBottomSheet
import com.poorrat.gotcha.ui.theme.PoorRatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    onNavigateToWeight: () -> Unit,
    onNavigateToWorkout: () -> Unit,
    onNavigateToSupplements: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onToggleTheme: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = PoorRatTheme.colors

    val dailySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val weeklySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val calcSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            PoorRatTopBar(
                onAddClick = { viewModel.openSheet(ActiveBottomSheet.DAILY_INTAKE) },
                onToggleTheme = onToggleTheme
            )
        },
        bottomBar = {
            PoorRatBottomBar(
                currentTab = "Alimentación",
                onTabSelected = { tab ->
                    when (tab) {
                        "Peso" -> onNavigateToWeight()
                        "Entreno" -> onNavigateToWorkout()
                        "Suplementos" -> onNavigateToSupplements()
                        "Tienda" -> onNavigateToShop()
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.background,
        modifier = modifier.fillMaxSize().testTag("nutrition_main_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Tarjeta Superior de Resumen (SEMANA vs DIA)
            WeeklyVsDailySummaryCard(
                weekCalories = uiState.weekTotalCalories,
                weekTarget = uiState.weekTargetCalories,
                todayCalories = uiState.todayCalories,
                dailyTarget = uiState.config.dailyCalorieTarget,
                remainingCalories = uiState.remainingCalories
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta de Progreso Diario y Macronutrientes
            DailyProgressAndMacrosCard(
                progressPercent = uiState.progressPercent,
                todayCalories = uiState.todayCalories,
                dailyTarget = uiState.config.dailyCalorieTarget,
                proteinGrams = uiState.todayProteinGrams,
                carbsGrams = uiState.todayCarbsGrams,
                fatGrams = uiState.todayFatGrams
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3 Tarjetas de Navegación a Submódulos
            ModuleNavCards(
                onDailyClick = { viewModel.openSheet(ActiveBottomSheet.DAILY_INTAKE) },
                onWeeklyClick = { viewModel.openSheet(ActiveBottomSheet.WEEKLY_INTAKE) },
                onCalculatorClick = { viewModel.openSheet(ActiveBottomSheet.CALORIE_CALCULATOR) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sub-vista 1: Consumo Diario ModalBottomSheet
        if (uiState.activeSheet == ActiveBottomSheet.DAILY_INTAKE) {
            DailyIntakeBottomSheet(
                uiState = uiState,
                sheetState = dailySheetState,
                onDismiss = { viewModel.closeSheet() },
                onSearchChange = { viewModel.updateSearchQuery(it) },
                onSelectFood = { viewModel.selectFood(it) },
                onClearSelectedFood = { viewModel.clearSelectedFood() },
                onGramsChange = { viewModel.updateGrams(it) },
                onManualValuesChange = { c, p, cb, f -> viewModel.updateManualValues(c, p, cb, f) },
                onRegisterMeal = { viewModel.registerFoodIntake() },
                onDeleteMeal = { viewModel.deleteFoodLog(it) }
            )
        }

        // Sub-vista 2: Consumo Semanal ModalBottomSheet
        if (uiState.activeSheet == ActiveBottomSheet.WEEKLY_INTAKE) {
            WeeklyIntakeBottomSheet(
                uiState = uiState,
                sheetState = weeklySheetState,
                onDismiss = { viewModel.closeSheet() }
            )
        }

        // Sub-vista 3: Calcular Consumo Calórico ModalBottomSheet
        if (uiState.activeSheet == ActiveBottomSheet.CALORIE_CALCULATOR) {
            CalorieCalculatorBottomSheet(
                uiState = uiState,
                sheetState = calcSheetState,
                onDismiss = { viewModel.closeSheet() },
                onAutoSyncToggle = { viewModel.setCalcAutoSyncWeight(it) },
                onSexChange = { viewModel.setCalcSex(it) },
                onWeightChange = { viewModel.setCalcWeight(it) },
                onHeightChange = { viewModel.setCalcHeight(it) },
                onAgeChange = { viewModel.setCalcAge(it) },
                onActivityChange = { viewModel.setCalcActivity(it) },
                onGoalChange = { viewModel.setCalcGoal(it) },
                onAdjustmentChange = { viewModel.setCalcAdjustment(it) },
                onApplyGoal = { viewModel.applyCalculatedGoal() }
            )
        }
    }
}
