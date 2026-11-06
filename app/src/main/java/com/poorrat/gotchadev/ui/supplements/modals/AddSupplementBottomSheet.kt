package com.poorrat.gotchadev.ui.supplements.modals

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotchadev.ui.supplements.SupplementsUiState
import com.poorrat.gotchadev.ui.supplements.SupplementsViewModel
import com.poorrat.gotchadev.ui.theme.PoorRatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplementBottomSheet(
    sheetState: SheetState,
    uiState: SupplementsUiState,
    onDismiss: () -> Unit,
    onTabChanged: (Int) -> Unit,
    onAiPromptChanged: (String) -> Unit,
    onEstimateAi: () -> Unit,
    onNameChanged: (String) -> Unit,
    onDosageChanged: (String) -> Unit,
    onIntakeMomentChanged: (String) -> Unit,
    onAlarmEnabledChanged: (Boolean) -> Unit,
    onAlarmTimeChanged: (Int, Int) -> Unit,
    onHasCaloriesChanged: (Boolean) -> Unit,
    onIsDailyConsumptionChanged: (Boolean) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onFatChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val colors = PoorRatTheme.colors
    val context = LocalContext.current
    var momentDropdownExpanded by remember { mutableStateOf(false) }

    val timePickerDialog = remember(context, uiState.addAlarmHour, uiState.addAlarmMinute) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onAlarmTimeChanged(hourOfDay, minute)
            },
            uiState.addAlarmHour,
            uiState.addAlarmMinute,
            false
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheetBackground,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(top = 28.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .background(colors.dragHandle, CircleShape)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Agregar Suplemento",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Añadir a tu protocolo diario",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_add_supplement")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector con únicamente 2 opciones: Ingreso con IA y Manual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Ingreso con IA
                val isAiSelected = uiState.addTab == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAiSelected) colors.surfaceCard else Color.Transparent)
                        .clickable { onTabChanged(0) }
                        .padding(vertical = 10.dp)
                        .testTag("tab_supplement_ai"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isAiSelected) colors.textPrimary else colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ingreso con IA",
                            fontSize = 13.sp,
                            fontWeight = if (isAiSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAiSelected) colors.textPrimary else colors.textMuted
                        )
                    }
                }

                // Tab 1: Ingreso Manual
                val isManualSelected = uiState.addTab == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isManualSelected) colors.surfaceCard else Color.Transparent)
                        .clickable { onTabChanged(1) }
                        .padding(vertical = 10.dp)
                        .testTag("tab_supplement_manual"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isManualSelected) colors.textPrimary else colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ingreso Manual",
                            fontSize = 13.sp,
                            fontWeight = if (isManualSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isManualSelected) colors.textPrimary else colors.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // TAB 0: ASISTENCIA IA
            if (uiState.addTab == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceCard)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Estimación inteligente del producto",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Escribe el nombre o los datos exactos del producto (ej. '1 scoop proteína whey con 120 kcal, 24g proteina', 'Péptidos de colágeno 10g', 'Creatina 5g').",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.addAiPrompt,
                            onValueChange = onAiPromptChanged,
                            placeholder = {
                                Text(
                                    "ej. Batido proteico 1 scoop, 130 kcal, 25g de proteína, 2g carbohidratos",
                                    color = colors.textMuted,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_supplement_ai_prompt"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.borderFocused,
                                unfocusedBorderColor = colors.border,
                                focusedContainerColor = colors.surfaceElevated,
                                unfocusedContainerColor = colors.surfaceElevated,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            minLines = 2,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onEstimateAi,
                            enabled = !uiState.isAiEstimating && uiState.addAiPrompt.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_estimate_supplement_ai"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.textPrimary,
                                contentColor = colors.background,
                                disabledContainerColor = colors.surfaceElevated,
                                disabledContentColor = colors.textMuted
                            )
                        ) {
                            if (uiState.isAiEstimating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.background
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Estimando con IA...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Estimar con IA", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // CAMPOS DEL SUPLEMENTO (Comunes a ambos, editables tras estimar o ingresar manual)
            Text(
                text = "NOMBRE DEL SUPLEMENTO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = uiState.addName,
                onValueChange = onNameChanged,
                placeholder = {
                    Text("ej. Proteína Whey Aislada o Ashwagandha", color = colors.textMuted, fontSize = 14.sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_supplement_name"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.borderFocused,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DOSIS RECOMENDADA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = uiState.addDosage,
                onValueChange = onDosageChanged,
                placeholder = {
                    Text("ej. 1 scoop (30g) o 5g", color = colors.textMuted, fontSize = 14.sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_supplement_dosage"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.borderFocused,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SECCIÓN CALORÍAS Y APORTE NUTRICIONAL
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = if (uiState.addHasCalories) Color(0xFFF97316) else colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "¿Añade calorías y macros?",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Batidos, péptidos, mass gainers, etc.",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = uiState.addHasCalories,
                            onCheckedChange = onHasCaloriesChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.background,
                                checkedTrackColor = colors.textPrimary,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceElevated,
                                uncheckedBorderColor = colors.border
                            ),
                            modifier = Modifier.testTag("switch_has_calories")
                        )
                    }

                    if (uiState.addHasCalories) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Switch para registrar consumo diario
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceElevated)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Marcar como consumo diario",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Suma sus calorías y macros al consumirlo cada día",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = uiState.addIsDailyConsumption,
                                onCheckedChange = onIsDailyConsumptionChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.background,
                                    checkedTrackColor = colors.textPrimary,
                                    uncheckedThumbColor = colors.textSecondary,
                                    uncheckedTrackColor = colors.surfaceElevated,
                                    uncheckedBorderColor = colors.border
                                ),
                                modifier = Modifier.testTag("switch_daily_consumption")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Campos de Calorías y Macronutrientes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Calorías
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CALORÍAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = uiState.addCalories,
                                    onValueChange = onCaloriesChanged,
                                    placeholder = { Text("0", color = colors.textMuted, fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth().testTag("input_supplement_calories"),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.borderFocused,
                                        unfocusedBorderColor = colors.border,
                                        focusedContainerColor = colors.surfaceElevated,
                                        unfocusedContainerColor = colors.surfaceElevated,
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary
                                    )
                                )
                            }

                            // Proteína
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PROTEÍNA (g)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = uiState.addProtein,
                                    onValueChange = onProteinChanged,
                                    placeholder = { Text("0", color = colors.textMuted, fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth().testTag("input_supplement_protein"),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.borderFocused,
                                        unfocusedBorderColor = colors.border,
                                        focusedContainerColor = colors.surfaceElevated,
                                        unfocusedContainerColor = colors.surfaceElevated,
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Carbos
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CARBOS (g)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = uiState.addCarbs,
                                    onValueChange = onCarbsChanged,
                                    placeholder = { Text("0", color = colors.textMuted, fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth().testTag("input_supplement_carbs"),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.borderFocused,
                                        unfocusedBorderColor = colors.border,
                                        focusedContainerColor = colors.surfaceElevated,
                                        unfocusedContainerColor = colors.surfaceElevated,
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary
                                    )
                                )
                            }

                            // Grasa
                            Column(modifier = Modifier.weight(1f)) {
                                Text("GRASA (g)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = uiState.addFat,
                                    onValueChange = onFatChanged,
                                    placeholder = { Text("0", color = colors.textMuted, fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth().testTag("input_supplement_fat"),
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.borderFocused,
                                        unfocusedBorderColor = colors.border,
                                        focusedContainerColor = colors.surfaceElevated,
                                        unfocusedContainerColor = colors.surfaceElevated,
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // MOMENTO DE LA TOMA
            Text(
                text = "MOMENTO DE LA TOMA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { momentDropdownExpanded = true }
                        .testTag("dropdown_intake_moment"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.addIntakeMoment,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = colors.textSecondary
                        )
                    }
                }

                DropdownMenu(
                    expanded = momentDropdownExpanded,
                    onDismissRequest = { momentDropdownExpanded = false },
                    modifier = Modifier.background(colors.surfaceElevated)
                ) {
                    SupplementsViewModel.INTAKE_MOMENTS.forEach { moment ->
                        DropdownMenuItem(
                            text = { Text(moment, fontSize = 14.sp, color = colors.textPrimary) },
                            onClick = {
                                onIntakeMomentChanged(moment)
                                momentDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta Alarma para este suplemento
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alarma para este suplemento",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Recibe un aviso a la hora indicada",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }

                        Switch(
                            checked = uiState.addAlarmEnabled,
                            onCheckedChange = onAlarmEnabledChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.background,
                                checkedTrackColor = colors.textPrimary,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surfaceElevated,
                                uncheckedBorderColor = colors.border
                            ),
                            modifier = Modifier.testTag("switch_add_alarm")
                        )
                    }

                    if (uiState.addAlarmEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HORA DE LA ALARMA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )

                            Card(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { timePickerDialog.show() }
                                    .testTag("add_alarm_time_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = SupplementsViewModel.formatTime12h(
                                            uiState.addAlarmHour,
                                            uiState.addAlarmMinute
                                        ),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.addErrorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = uiState.addErrorMessage,
                    color = Color(0xFFF43F5E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Botón Inferior: Añadir al Stack Diario
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_add_supplement"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.textPrimary,
                    contentColor = colors.background
                )
            ) {
                Text(
                    text = "Añadir al Stack Diario",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
