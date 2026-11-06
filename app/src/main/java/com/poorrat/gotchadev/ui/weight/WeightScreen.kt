package com.poorrat.gotchadev.ui.weight

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poorrat.gotchadev.ui.nutrition.components.PoorRatBottomBar
import com.poorrat.gotchadev.ui.theme.PoorRatTheme
import com.poorrat.gotchadev.ui.weight.components.PoorRatWeightTopBar
import com.poorrat.gotchadev.ui.weight.components.WeightNavCards
import com.poorrat.gotchadev.ui.weight.components.WeightSummaryCard
import com.poorrat.gotchadev.ui.weight.modals.EditRegisterWeightBottomSheet
import com.poorrat.gotchadev.ui.weight.modals.WeightHistoryBottomSheet
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    viewModel: WeightViewModel,
    onNavigateToNutrition: () -> Unit,
    onNavigateToWorkout: () -> Unit,
    onNavigateToSupplements: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onToggleTheme: (() -> Unit)? = null,
    onLogoClick: (() -> Unit)? = null,
    weightUnit: com.poorrat.gotchadev.data.preferences.WeightUnit = com.poorrat.gotchadev.data.preferences.WeightUnit.KG,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = PoorRatTheme.colors

    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showEditTargetDialog by remember { mutableStateOf(false) }
    var inputTargetWeightText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            PoorRatWeightTopBar(
                onAddClick = { viewModel.openSheet(WeightActiveSheet.EDIT_REGISTER_WEIGHT) },
                onToggleTheme = onToggleTheme,
                onLogoClick = onLogoClick
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
                differenceKg = uiState.differenceKg,
                unit = weightUnit,
                onEditTargetWeightClick = {
                    inputTargetWeightText = String.format(Locale.US, "%.1f", uiState.targetWeightKg)
                    showEditTargetDialog = true
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2 Tarjetas de Navegación a Submódulos
            WeightNavCards(
                onHistoryClick = { viewModel.openSheet(WeightActiveSheet.WEIGHT_HISTORY) },
                onEditRegisterClick = { viewModel.openSheet(WeightActiveSheet.EDIT_REGISTER_WEIGHT) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showEditTargetDialog) {
            AlertDialog(
                onDismissRequest = { showEditTargetDialog = false },
                title = {
                    Text(
                        text = "Editar Peso Meta",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Ingresa tu nuevo peso objetivo (${weightUnit.symbol}):",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputTargetWeightText,
                            onValueChange = { inputTargetWeightText = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.buttonPrimary,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newTarget = inputTargetWeightText.replace(',', '.').toFloatOrNull()
                            if (newTarget != null && newTarget > 0f) {
                                val targetKg = if (weightUnit == com.poorrat.gotchadev.data.preferences.WeightUnit.LB) newTarget / 2.20462f else newTarget
                                viewModel.updateTargetWeight(targetKg)
                            }
                            showEditTargetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.buttonPrimary,
                            contentColor = colors.buttonPrimaryText
                        )
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTargetDialog = false }) {
                        Text("Cancelar", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surfaceElevated
            )
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
