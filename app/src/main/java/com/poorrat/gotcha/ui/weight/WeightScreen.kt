package com.poorrat.gotcha.ui.weight

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poorrat.gotcha.ui.nutrition.components.PoorRatBottomBar
import com.poorrat.gotcha.ui.theme.PoorRatTheme
import com.poorrat.gotcha.ui.weight.components.PoorRatWeightTopBar
import com.poorrat.gotcha.ui.weight.components.WeightNavCards
import com.poorrat.gotcha.ui.weight.components.WeightSummaryCard
import com.poorrat.gotcha.ui.weight.modals.EditRegisterWeightBottomSheet
import com.poorrat.gotcha.ui.weight.modals.WeightHistoryBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    viewModel: WeightViewModel,
    onNavigateToNutrition: () -> Unit,
    onNavigateToWorkout: () -> Unit,
    onNavigateToSupplements: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onToggleTheme: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = PoorRatTheme.colors

    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            PoorRatWeightTopBar(
                onAddClick = { viewModel.openSheet(WeightActiveSheet.EDIT_REGISTER_WEIGHT) },
                onToggleTheme = onToggleTheme
            )
        },
        bottomBar = {
            PoorRatBottomBar(
                currentTab = "Peso",
                onTabSelected = { tab ->
                    when (tab) {
                        "Alimentación" -> onNavigateToNutrition()
                        "Entreno" -> onNavigateToWorkout()
                        "Suplementos" -> onNavigateToSupplements()
                        "Tienda" -> onNavigateToShop()
                    }
                }
            )
        },
        containerColor = colors.background,
        modifier = modifier.fillMaxSize().testTag("weight_main_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Tarjeta Superior de Resumen (PESO ACTUAL vs PESO META)
            WeightSummaryCard(
                currentWeightKg = uiState.currentWeightKg,
                targetWeightKg = uiState.targetWeightKg,
                differenceKg = uiState.differenceKg
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2 Tarjetas de Navegación a Submódulos
            WeightNavCards(
                onHistoryClick = { viewModel.openSheet(WeightActiveSheet.WEIGHT_HISTORY) },
                onEditRegisterClick = { viewModel.openSheet(WeightActiveSheet.EDIT_REGISTER_WEIGHT) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sub-vista 1: Seguimiento de Peso ModalBottomSheet
        if (uiState.activeSheet == WeightActiveSheet.WEIGHT_HISTORY) {
            WeightHistoryBottomSheet(
                uiState = uiState,
                sheetState = historySheetState,
                onDismiss = { viewModel.closeSheet() },
                onDeleteLog = { viewModel.deleteWeightRecord(it) }
            )
        }

        // Sub-vista 2: Editar / Registrar Peso ModalBottomSheet
        if (uiState.activeSheet == WeightActiveSheet.EDIT_REGISTER_WEIGHT) {
            EditRegisterWeightBottomSheet(
                uiState = uiState,
                sheetState = editSheetState,
                onDismiss = { viewModel.closeSheet() },
                onWeightChange = { viewModel.updateInputWeight(it) },
                onTargetWeightChange = { viewModel.updateInputTargetWeight(it) },
                onNoteChange = { viewModel.updateInputNote(it) },
                onSave = { viewModel.saveWeightRecord() }
            )
        }
    }
}
