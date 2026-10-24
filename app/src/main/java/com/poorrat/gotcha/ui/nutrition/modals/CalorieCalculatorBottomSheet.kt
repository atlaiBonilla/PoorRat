package com.poorrat.gotcha.ui.nutrition.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.ui.nutrition.NutritionUiState
import com.poorrat.gotcha.ui.theme.PoorRatTheme
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieCalculatorBottomSheet(
    uiState: NutritionUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAutoSyncToggle: (Boolean) -> Unit,
    onSexChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onActivityChange: (String) -> Unit,
    onGoalChange: (String) -> Unit,
    onAdjustmentChange: (String) -> Unit,
    onApplyGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val colors = PoorRatTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheetBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .background(colors.sheetDragHandle, CircleShape)
            )
        },
        modifier = modifier.testTag("calorie_calculator_bottom_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Title, Subtitle, Close Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Calcular Consumo Calórico",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Calculadora TDEE / Mifflin St Jeor",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_calorie_calc_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // SEXO (Selector de Sexo Biológico)
            item {
                Text(
                    text = "SEXO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectableOptionCard(
                        title = "Masculino",
                        subtitle = "Ajuste Mifflin: +5",
                        isSelected = uiState.calcSex == "MALE",
                        onClick = { onSexChange("MALE") },
                        modifier = Modifier.weight(1f)
                    )

                    SelectableOptionCard(
                        title = "Femenino",
                        subtitle = "Ajuste Mifflin: -161",
                        isSelected = uiState.calcSex == "FEMALE",
                        onClick = { onSexChange("FEMALE") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Switch Toggle: Usar peso actual del módulo de Peso
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auto_sync_weight_toggle_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceCard
                    ),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Usar peso actual del módulo de Peso",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiState.calcAutoSyncWeight) {
                                    "Sincronizado: ${String.format(Locale.US, "%.1f", uiState.latestLoggedWeight)} kg (bloqueado para evitar discrepancias)"
                                } else {
                                    "Modo simulación (edición manual libre)"
                                },
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }

                        Switch(
                            checked = uiState.calcAutoSyncWeight,
                            onCheckedChange = onAutoSyncToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.buttonPrimaryText,
                                checkedTrackColor = colors.buttonPrimaryBg,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceElevated,
                                uncheckedBorderColor = colors.border
                            ),
                            modifier = Modifier.testTag("auto_sync_weight_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Datos Físicos: Peso (kg), Altura (cm), Edad
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PhysicalInputBox(
                        label = if (uiState.calcAutoSyncWeight) "Peso (kg) 🔒" else "Peso (kg)",
                        value = uiState.calcWeightKg,
                        isReadOnly = uiState.calcAutoSyncWeight,
                        onValueChange = onWeightChange,
                        onIncrement = {
                            if (!uiState.calcAutoSyncWeight) {
                                val cur = uiState.calcWeightKg.replace(',', '.').toFloatOrNull() ?: 70f
                                onWeightChange(String.format(Locale.US, "%.1f", cur + 0.5f))
                            }
                        },
                        onDecrement = {
                            if (!uiState.calcAutoSyncWeight) {
                                val cur = uiState.calcWeightKg.replace(',', '.').toFloatOrNull() ?: 70f
                                if (cur > 10f) onWeightChange(String.format(Locale.US, "%.1f", cur - 0.5f))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    PhysicalInputBox(
                        label = "Altura (cm)",
                        value = uiState.calcHeightCm,
                        isReadOnly = false,
                        onValueChange = onHeightChange,
                        onIncrement = {
                            val cur = uiState.calcHeightCm.toIntOrNull() ?: 170
                            onHeightChange((cur + 1).toString())
                        },
                        onDecrement = {
                            val cur = uiState.calcHeightCm.toIntOrNull() ?: 170
                            if (cur > 50) onHeightChange((cur - 1).toString())
                        },
                        modifier = Modifier.weight(1f)
                    )

                    PhysicalInputBox(
                        label = "Edad",
                        value = uiState.calcAge,
                        isReadOnly = false,
                        onValueChange = onAgeChange,
                        onIncrement = {
                            val cur = uiState.calcAge.toIntOrNull() ?: 25
                            onAgeChange((cur + 1).toString())
                        },
                        onDecrement = {
                            val cur = uiState.calcAge.toIntOrNull() ?: 25
                            if (cur > 10) onAgeChange((cur - 1).toString())
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // ACTIVIDAD FÍSICA (2x2 Grid)
            item {
                Text(
                    text = "ACTIVIDAD FÍSICA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelectableOptionCard(
                        title = "Nula",
                        subtitle = "Sedentaria",
                        isSelected = uiState.calcActivityLevel == "SEDENTARY",
                        onClick = { onActivityChange("SEDENTARY") },
                        modifier = Modifier.weight(1f)
                    )
                    SelectableOptionCard(
                        title = "Ligera",
                        subtitle = "1–2 días",
                        isSelected = uiState.calcActivityLevel == "LIGHT",
                        onClick = { onActivityChange("LIGHT") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelectableOptionCard(
                        title = "Moderada",
                        subtitle = "3–5 días",
                        isSelected = uiState.calcActivityLevel == "MODERATE",
                        onClick = { onActivityChange("MODERATE") },
                        modifier = Modifier.weight(1f)
                    )
                    SelectableOptionCard(
                        title = "Alta",
                        subtitle = "6–7 días",
                        isSelected = uiState.calcActivityLevel == "HIGH",
                        onClick = { onActivityChange("HIGH") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // OBJETIVO (Déficit / Mantenimiento / Superávit)
            item {
                Text(
                    text = "OBJETIVO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelectableOptionCard(
                        title = "Déficit",
                        subtitle = "Restar calorías",
                        isSelected = uiState.calcGoalType == "DEFICIT",
                        onClick = { onGoalChange("DEFICIT") },
                        modifier = Modifier.weight(1f)
                    )
                    SelectableOptionCard(
                        title = "Superávit",
                        subtitle = "Agregar calorías",
                        isSelected = uiState.calcGoalType == "SURPLUS",
                        onClick = { onGoalChange("SURPLUS") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                SelectableOptionCard(
                    title = "Mantenimiento",
                    subtitle = "Gasto calórico equilibrado (TDEE exacto)",
                    isSelected = uiState.calcGoalType == "MAINTENANCE",
                    onClick = { onGoalChange("MAINTENANCE") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Calorías de Ajuste (solo si no es mantenimiento)
            if (uiState.calcGoalType != "MAINTENANCE") {
                item {
                    val labelAdjustment = if (uiState.calcGoalType == "DEFICIT") "CALORÍAS A RESTAR" else "CALORÍAS A AGREGAR"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = labelAdjustment,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.inputBackground),
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = uiState.calcAdjustmentCalories,
                                            onValueChange = onAdjustmentChange,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(72.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedTextColor = colors.textPrimary,
                                                unfocusedTextColor = colors.textPrimary
                                            )
                                        )

                                        Column {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = null,
                                                tint = colors.textPrimary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        val cur = uiState.calcAdjustmentCalories.toIntOrNull() ?: 350
                                                        onAdjustmentChange((cur + 50).toString())
                                                    }
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = colors.textPrimary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        val cur = uiState.calcAdjustmentCalories.toIntOrNull() ?: 350
                                                        if (cur >= 50) onAdjustmentChange((cur - 50).toString())
                                                    }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "kcal", fontSize = 13.sp, color = colors.textSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // Live Calculated Metrics Card (METABOLISMO BASAL, GASTO DIARIO, META DIARIA)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_results_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = 18.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ResultColumn(
                            label = "MET. BASAL",
                            value = "${numberFormat.format(uiState.previewBmr)} kcal",
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                            color = colors.divider
                        ) {}

                        ResultColumn(
                            label = "GASTO (TDEE)",
                            value = "${numberFormat.format(uiState.previewTdee)} kcal",
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                            color = colors.divider
                        ) {}

                        ResultColumn(
                            label = "META DIARIA",
                            value = "${numberFormat.format(uiState.previewTarget)} kcal",
                            isHighlight = true,
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Primary Apply Goal Button
            item {
                Button(
                    onClick = onApplyGoal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("apply_calorie_goal_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.buttonPrimaryBg,
                        contentColor = colors.buttonPrimaryText
                    )
                ) {
                    Text(
                        text = "APLICAR META (${numberFormat.format(uiState.previewTarget)} KCAL)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    val isDark = colors.isDark

    val containerBg = if (isSelected) {
        if (isDark) Color.White else Color(0xFF111111)
    } else {
        colors.surfaceCard
    }

    val titleColor = if (isSelected) {
        if (isDark) Color.Black else Color.White
    } else {
        colors.textPrimary
    }

    val subColor = if (isSelected) {
        if (isDark) Color(0xFF52525B) else Color(0xFFC7C7CC)
    } else {
        colors.textSecondary
    }

    val borderStroke = if (isSelected) {
        BorderStroke(1.dp, if (isDark) Color.White else Color(0xFF111111))
    } else {
        BorderStroke(1.dp, colors.border)
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("option_${title.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = subColor
            )
        }
    }
}

@Composable
private fun PhysicalInputBox(
    label: String,
    value: String,
    isReadOnly: Boolean = false,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isReadOnly) colors.textPrimary else colors.textSecondary,
            fontWeight = if (isReadOnly) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.inputBackground
            ),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = isReadOnly,
                    enabled = !isReadOnly,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        disabledTextColor = colors.textPrimary,
                        disabledContainerColor = colors.inputBackground,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                if (!isReadOnly) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(onClick = onIncrement)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(onClick = onDecrement)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultColumn(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = if (isHighlight) 22.sp else 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary
        )
    }
}
