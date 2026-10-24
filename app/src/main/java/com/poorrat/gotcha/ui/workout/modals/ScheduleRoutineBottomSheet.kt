package com.poorrat.gotcha.ui.workout.modals

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.data.local.entity.RoutineExerciseEntity
import com.poorrat.gotcha.ui.theme.PoorRatTheme
import com.poorrat.gotcha.ui.workout.WorkoutUiState
import com.poorrat.gotcha.ui.workout.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRoutineBottomSheet(
    uiState: WorkoutUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onRoutineNameChange: (String) -> Unit,
    onBodyPartChange: (String) -> Unit,
    onNewExNameChange: (String) -> Unit,
    onNewExSetsChange: (String) -> Unit,
    onNewExRepsChange: (String) -> Unit,
    onNewExWeightChange: (String) -> Unit,
    onAddExercise: () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onSaveSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    val bodyPartOptions = listOf(
        "Piernas",
        "Empuje",
        "Jalón",
        "Upper",
        "Lower",
        "Brazos",
        "Descanso"
    )

    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var bodyPartDropdownExpanded by remember { mutableStateOf(false) }

    val isRestDay = uiState.scheduleBodyPart == "Descanso"

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
        modifier = modifier.testTag("schedule_routine_bottom_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Programar Rutina, Close Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Programar Rutina",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_schedule_routine_modal")
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

            // 1. Nombre de la rutina (OutlinedTextField)
            item {
                OutlinedTextField(
                    value = uiState.scheduleRoutineName,
                    onValueChange = onRoutineNameChange,
                    placeholder = { Text("Nombre de la rutina (ej. Upper de Fuerza)", fontSize = 14.sp, color = colors.textMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_routine_name_input"),
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

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2. Selector de Día (Dropdown)
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { dayDropdownExpanded = true }
                            .testTag("schedule_day_selector"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = WorkoutViewModel.getDayLabel(uiState.scheduleSelectedDayOfWeek),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
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
                        expanded = dayDropdownExpanded,
                        onDismissRequest = { dayDropdownExpanded = false },
                        modifier = Modifier.background(colors.surfaceElevated)
                    ) {
                        (1..7).forEach { dayNum ->
                            DropdownMenuItem(
                                text = { Text(WorkoutViewModel.getDayLabel(dayNum), fontSize = 14.sp, color = colors.textPrimary) },
                                onClick = {
                                    onSelectDay(dayNum)
                                    dayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Contenedor de Enfoque y Ejercicios
            item {
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
                        // Dropdown Parte del cuerpo
                        Text(
                            text = "Parte del cuerpo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { bodyPartDropdownExpanded = true }
                                    .testTag("body_part_dropdown"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = uiState.scheduleBodyPart,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isRestDay) colors.textSecondary else colors.textPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = colors.textSecondary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = bodyPartDropdownExpanded,
                                onDismissRequest = { bodyPartDropdownExpanded = false },
                                modifier = Modifier.background(colors.surfaceElevated)
                            ) {
                                bodyPartOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option,
                                                fontSize = 14.sp,
                                                fontWeight = if (option == "Descanso") FontWeight.Bold else FontWeight.Normal,
                                                color = if (option == "Descanso") colors.textSecondary else colors.textPrimary
                                            )
                                        },
                                        onClick = {
                                            onBodyPartChange(option)
                                            bodyPartDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Regla de Bloqueo por "Descanso"
                        if (isRestDay) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text(
                                    text = "Día de descanso seleccionado. No se asignan ejercicios a este día.",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            // Sub-sección Ejercicios cuando NO es descanso
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Ejercicios · Día ${uiState.scheduleSelectedDayOfWeek}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Lista de ejercicios ya añadidos
                            if (uiState.scheduleExercises.isEmpty()) {
                                Text(
                                    text = "No hay ejercicios agregados aún. Agrega uno abajo.",
                                    fontSize = 12.sp,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                uiState.scheduleExercises.forEachIndexed { index, exercise ->
                                    ScheduleExerciseItem(
                                        exercise = exercise,
                                        onDelete = { onRemoveExercise(index) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sub-formulario para añadir ejercicio
                            OutlinedTextField(
                                value = uiState.newExerciseName,
                                onValueChange = onNewExNameChange,
                                placeholder = { Text("Nuevo ejercicio", fontSize = 13.sp, color = colors.textMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_exercise_name_input"),
                                shape = RoundedCornerShape(10.dp),
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fila Series & Repeticiones
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Series with controls
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        OutlinedTextField(
                                            value = uiState.newExerciseSets,
                                            onValueChange = onNewExSetsChange,
                                            placeholder = { Text("Series", fontSize = 12.sp, color = colors.textMuted) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
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
                                                tint = colors.textSecondary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        val cur = uiState.newExerciseSets.toIntOrNull() ?: 3
                                                        onNewExSetsChange((cur + 1).toString())
                                                    }
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = colors.textSecondary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        val cur = uiState.newExerciseSets.toIntOrNull() ?: 3
                                                        if (cur > 1) onNewExSetsChange((cur - 1).toString())
                                                    }
                                            )
                                        }
                                    }
                                }

                                // Repeticiones
                                OutlinedTextField(
                                    value = uiState.newExerciseReps,
                                    onValueChange = onNewExRepsChange,
                                    placeholder = { Text("Repeticiones", fontSize = 12.sp, color = colors.textMuted) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
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
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fila Peso objetivo & Botón Agregar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = uiState.newExerciseWeight,
                                    onValueChange = onNewExWeightChange,
                                    placeholder = { Text("Peso objetivo (kg)", fontSize = 12.sp, color = colors.textMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
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

                                Button(
                                    onClick = onAddExercise,
                                    enabled = uiState.newExerciseName.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("add_exercise_to_schedule_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.surfaceElevated,
                                        contentColor = colors.textPrimary,
                                        disabledContainerColor = colors.surfaceCard,
                                        disabledContentColor = colors.textMuted
                                    ),
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Text("Agregar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Botón Inferior: Guardar programación
            item {
                Button(
                    onClick = onSaveSchedule,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_schedule_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.textPrimary,
                        contentColor = colors.background
                    )
                ) {
                    Text(
                        text = "Guardar programación",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleExerciseItem(
    exercise: RoutineExerciseEntity,
    onDelete: () -> Unit
) {
    val colors = PoorRatTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${exercise.targetSets} x ${exercise.targetReps} · ${exercise.targetWeightKg.toString().replace(".0", "")} kg",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
