package com.poorrat.gotcha.ui.supplements.modals

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.ui.supplements.SupplementsUiState
import com.poorrat.gotcha.ui.supplements.SupplementsViewModel
import com.poorrat.gotcha.ui.theme.PoorRatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplementBottomSheet(
    sheetState: SheetState,
    uiState: SupplementsUiState,
    onDismiss: () -> Unit,
    onNameChanged: (String) -> Unit,
    onDosageChanged: (String) -> Unit,
    onIntakeMomentChanged: (String) -> Unit,
    onAlarmEnabledChanged: (Boolean) -> Unit,
    onAlarmTimeChanged: (Int, Int) -> Unit,
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
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .background(colors.dragHandle, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
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

            Spacer(modifier = Modifier.height(20.dp))

            // 1. NOMBRE DEL SUPLEMENTO
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
                    Text("ej. Ashwagandha KSM-66", color = colors.textMuted, fontSize = 14.sp)
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

            Spacer(modifier = Modifier.height(18.dp))

            // 2. DOSIS RECOMENDADA
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
                    Text("ej. 600 mg o 1 cápsula", color = colors.textMuted, fontSize = 14.sp)
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

            // 3. MOMENTO DE LA TOMA
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

            // 4. Tarjeta Alarma para este suplemento
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

                    // Selector de hora si la alarma está encendida
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
