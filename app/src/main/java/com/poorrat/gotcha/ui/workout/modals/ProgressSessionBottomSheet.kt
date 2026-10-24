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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.data.local.entity.RoutineExerciseEntity
import com.poorrat.gotcha.ui.theme.PoorRatTheme
import com.poorrat.gotcha.ui.workout.ExerciseSessionInput
import com.poorrat.gotcha.ui.workout.WorkoutUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressSessionBottomSheet(
    uiState: WorkoutUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onExerciseCompletedChange: (Long, Boolean) -> Unit,
    onExerciseWeightChange: (Long, String) -> Unit,
    onExerciseRepsChange: (Long, String) -> Unit,
    onFinishSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    val totalSeconds = uiState.sessionElapsedSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timerFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

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
        modifier = modifier.testTag("progress_session_bottom_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: PROGRESO, Close Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PROGRESO",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_progress_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // TIEMPO DE SESIÓN Card (Imagen 10)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_timer_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = BorderStroke(1.dp, colors.border),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TIEMPO DE SESIÓN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = timerFormatted,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Exercise Cards List
            items(uiState.todayExercises, key = { it.id }) { exercise ->
                val input = uiState.sessionExerciseInputs[exercise.id] ?: ExerciseSessionInput(
                    isCompleted = false,
                    achievedWeightText = exercise.targetWeightKg.toString().replace(".0", ""),
                    achievedRepsText = exercise.targetReps
                )

                ExerciseProgressCard(
                    exercise = exercise,
                    input = input,
                    onCompletedChange = { onExerciseCompletedChange(exercise.id, it) },
                    onWeightChange = { onExerciseWeightChange(exercise.id, it) },
                    onRepsChange = { onExerciseRepsChange(exercise.id, it) }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Botón Inferior: Guardar y Finalizar
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onFinishSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_and_finish_workout_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.textPrimary,
                        contentColor = colors.background
                    )
                ) {
                    Text(
                        text = "Guardar y Finalizar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseProgressCard(
    exercise: RoutineExerciseEntity,
    input: ExerciseSessionInput,
    onCompletedChange: (Boolean) -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit
) {
    val colors = PoorRatTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_progress_card_${exercise.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Name, Target, Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Objetivo: ${exercise.targetSets} x ${exercise.targetReps} · ${exercise.targetWeightKg.toString().replace(".0", "")} kg",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                Checkbox(
                    checked = input.isCompleted,
                    onCheckedChange = onCompletedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.textPrimary,
                        uncheckedColor = colors.borderFocused,
                        checkmarkColor = colors.background
                    ),
                    modifier = Modifier.testTag("checkbox_exercise_${exercise.id}")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Row: Peso logrado & Reps logradas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Peso logrado
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    OutlinedTextField(
                        value = input.achievedWeightText,
                        onValueChange = { onWeightChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
                        placeholder = { Text("Peso logrado", fontSize = 12.sp, color = colors.textMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }

                // Reps logradas with Up/Down controls
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
                            value = input.achievedRepsText,
                            onValueChange = onRepsChange,
                            placeholder = { Text("Reps logradas", fontSize = 12.sp, color = colors.textMuted) },
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

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Aumentar reps",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        val cur = input.achievedRepsText.toIntOrNull() ?: 10
                                        onRepsChange((cur + 1).toString())
                                    }
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Disminuir reps",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        val cur = input.achievedRepsText.toIntOrNull() ?: 10
                                        if (cur > 1) onRepsChange((cur - 1).toString())
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
