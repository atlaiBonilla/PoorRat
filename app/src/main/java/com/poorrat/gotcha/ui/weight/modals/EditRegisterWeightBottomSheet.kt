package com.poorrat.gotcha.ui.weight.modals

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.ui.theme.PoorRatTheme
import com.poorrat.gotcha.ui.weight.WeightUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRegisterWeightBottomSheet(
    uiState: WeightUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onWeightChange: (String) -> Unit,
    onTargetWeightChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    .background(colors.dragHandle, CircleShape)
            )
        },
        modifier = modifier.testTag("edit_register_weight_bottom_sheet")
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
                            text = "Editar / Registrar Peso",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Ingresar nueva lectura de pesaje",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_edit_weight_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 1. PESO ACTUAL DE HOY (KG)
            item {
                Text(
                    text = "PESO ACTUAL DE HOY (KG)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                CenteredNumericInputBox(
                    value = uiState.inputWeightKg,
                    onValueChange = onWeightChange,
                    onIncrement = {
                        val cur = uiState.inputWeightKg.replace(',', '.').toFloatOrNull() ?: 70f
                        onWeightChange(String.format(Locale.US, "%.1f", cur + 0.1f))
                    },
                    onDecrement = {
                        val cur = uiState.inputWeightKg.replace(',', '.').toFloatOrNull() ?: 70f
                        if (cur > 10f) onWeightChange(String.format(Locale.US, "%.1f", cur - 0.1f))
                    },
                    modifier = Modifier.testTag("input_current_weight_box")
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 2. PESO OBJETIVO / META (KG)
            item {
                Text(
                    text = "PESO OBJETIVO / META (KG)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                CenteredNumericInputBox(
                    value = uiState.inputTargetWeightKg,
                    onValueChange = onTargetWeightChange,
                    onIncrement = {
                        val cur = uiState.inputTargetWeightKg.replace(',', '.').toFloatOrNull() ?: 70f
                        onTargetWeightChange(String.format(Locale.US, "%.1f", cur + 0.5f))
                    },
                    onDecrement = {
                        val cur = uiState.inputTargetWeightKg.replace(',', '.').toFloatOrNull() ?: 70f
                        if (cur > 10f) onTargetWeightChange(String.format(Locale.US, "%.1f", cur - 0.5f))
                    },
                    modifier = Modifier.testTag("input_target_weight_box")
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. NOTA O CONDICIÓN
            item {
                Text(
                    text = "NOTA O CONDICIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.inputNote,
                    onValueChange = onNoteChange,
                    placeholder = {
                        Text("En ayunas, post entreno, inicio de semana...", color = colors.textMuted, fontSize = 14.sp)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.borderFocused,
                        unfocusedBorderColor = colors.border,
                        focusedContainerColor = colors.surfaceElevated,
                        unfocusedContainerColor = colors.surfaceElevated,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_weight_note"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Botón Guardar Registro de Peso
            item {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_weight_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.textPrimary,
                        contentColor = colors.background
                    )
                ) {
                    Text(
                        text = "Guardar Registro de Peso",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredNumericInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.width(24.dp))

            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = colors.textPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Aumentar",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onIncrement)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Disminuir",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onDecrement)
                )
            }
        }
    }
}
