package com.poorrat.gotcha.ui.workout.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.data.local.entity.RoutineExerciseEntity
import com.poorrat.gotcha.ui.theme.PoorRatTheme
import com.poorrat.gotcha.ui.workout.WorkoutUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailBottomSheet(
    uiState: WorkoutUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    val dayLabelsShort = listOf(
        1 to ("1" to "LU"),
        2 to ("2" to "MA"),
        3 to ("3" to "MI"),
        4 to ("4" to "JU"),
        5 to ("5" to "VI"),
        6 to ("6" to "SÁ"),
        7 to ("7" to "DO")
    )

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
        modifier = modifier.testTag("routine_detail_bottom_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Title & Close Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Detalle de Rutina",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_routine_detail_modal")
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

            // 7-day selector horizontal row (Imagen 11)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dayLabelsShort.forEach { (dayNum, labels) ->
                        val isSelected = uiState.detailSelectedDayOfWeek == dayNum
                        DaySelectorButton(
                            number = labels.first,
                            name = labels.second,
                            isSelected = isSelected,
                            onClick = { onSelectDay(dayNum) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Encabezado del Día Seleccionado
            item {
                val dayEntity = uiState.detailRoutineDay
                Text(
                    text = "DETALLE DE RUTINA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dayEntity?.dayLabel ?: "Día 1 · Lunes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = dayEntity?.routineName ?: "Upper de fuerza",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Lista de Ejercicios
            val isRest = uiState.detailRoutineDay?.bodyPart == "Descanso"
            if (isRest || uiState.detailExercises.isEmpty()) {
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Hotel,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Día de Descanso",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sin ejercicios programados para este día",
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.detailExercises, key = { it.id }) { exercise ->
                    RoutineDetailExerciseItem(exercise = exercise)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun DaySelectorButton(
    number: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = PoorRatTheme.colors

    Card(
        modifier = Modifier
            .size(width = 44.dp, height = 54.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("day_button_$number"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.textPrimary else colors.surfaceElevated
        ),
        border = BorderStroke(1.dp, if (isSelected) colors.textPrimary else colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = number,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) colors.background else colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) colors.background.copy(alpha = 0.7f) else colors.textSecondary
            )
        }
    }
}

@Composable
private fun RoutineDetailExerciseItem(
    exercise: RoutineExerciseEntity
) {
    val colors = PoorRatTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("routine_detail_exercise_${exercise.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Numbered circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(colors.surfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = exercise.exerciseOrder.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = exercise.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${exercise.targetSets} series x ${exercise.targetReps} reps",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Text(
                text = "${exercise.targetWeightKg.toString().replace(".0", "")} kg",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }
    }
}
