package com.poorrat.gotcha.ui.nutrition.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.ui.nutrition.NutritionUiState
import com.poorrat.gotcha.ui.nutrition.WeeklyDayData
import com.poorrat.gotcha.ui.theme.PoorRatTheme
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyIntakeBottomSheet(
    uiState: NutritionUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
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
        modifier = modifier.testTag("weekly_intake_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Title, Subtitle, Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Consumo Semanal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Historial de 7 días",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("close_weekly_intake_modal")
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

            // Resumen Card: ACUMULADO SEMANA vs PROMEDIO DIARIO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_summary_metrics_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Column: ACUMULADO SEMANA
                    Column {
                        Text(
                            text = "ACUMULADO SEMANA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = numberFormat.format(uiState.weekTotalCalories),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "kcal",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    // Right Column: PROMEDIO DIARIO
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PROMEDIO DIARIO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = numberFormat.format(uiState.weekAverageCalories),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "kcal/día",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Gráfico Histórico 7 días Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_history_chart_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Chart Top Bar: "Histórico 7 días" and "Meta: X kcal"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Histórico 7 días",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "Meta: ${numberFormat.format(uiState.config.dailyCalorieTarget)} kcal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 7-Bar Chart rendering
                    WeeklyBarsChart(
                        days = uiState.weeklyDays,
                        targetCalories = uiState.config.dailyCalorieTarget
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Balance energético de la semana
            val diffTarget = uiState.weekAverageCalories - uiState.config.dailyCalorieTarget
            val diffText = if (diffTarget <= 0) {
                "$diffTarget kcal/día"
            } else {
                "+$diffTarget kcal/día"
            }
            val balanceStyle = buildAnnotatedString {
                append("El balance energético de la semana se mantiene en ")
                withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold)) {
                    append(diffText)
                }
                append(" respecto a tu meta diaria.")
            }

            Text(
                text = balanceStyle,
                fontSize = 12.sp,
                color = colors.textSecondary,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun WeeklyBarsChart(
    days: List<WeeklyDayData>,
    targetCalories: Int,
    modifier: Modifier = Modifier
) {
    val maxCalories = days.maxOfOrNull { it.calories } ?: 0
    val maxReference = maxOf(targetCalories * 1.22f, maxCalories * 1.15f, 2500f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            WeeklyDayBarColumn(
                day = day,
                targetCalories = targetCalories,
                maxReference = maxReference,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeeklyDayBarColumn(
    day: WeeklyDayData,
    targetCalories: Int,
    maxReference: Float,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    val barHeightRatio = if (day.hasLogs) (day.calories / maxReference).coerceIn(0.08f, 1f) else 0.12f
    val targetRatio = (targetCalories / maxReference).coerceIn(0.1f, 0.95f)

    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Value above bar: e.g. "2150" or "-"
        Text(
            text = if (day.hasLogs) "${day.calories}" else "-",
            fontSize = 11.sp,
            fontWeight = if (day.hasLogs) FontWeight.Bold else FontWeight.Medium,
            color = if (day.hasLogs) colors.textPrimary else colors.textSecondary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Bar with target indicator
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(150.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val barWidth = size.width
                val barHeight = size.height * barHeightRatio
                val targetY = size.height * (1f - targetRatio)

                if (day.hasLogs) {
                    // Solid rounded bar (White in dark mode, Dark in light mode)
                    drawRoundRect(
                        color = colors.progressFill,
                        topLeft = Offset(0f, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Target indicator dot / notch on the bar
                    if (targetY >= (size.height - barHeight)) {
                        // Exceeded: draw thin dividing line across bar
                        drawLine(
                            color = if (colors.isDark) Color(0xFF1E1E1E) else Color.White,
                            start = Offset(2.dp.toPx(), targetY),
                            end = Offset(barWidth - 2.dp.toPx(), targetY),
                            strokeWidth = 2.5f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    } else {
                        // Below target: draw target indicator dot
                        drawCircle(
                            color = colors.textSecondary,
                            radius = 3.dp.toPx(),
                            center = Offset(barWidth / 2f, targetY)
                        )
                    }
                } else {
                    // Day has NO logs: dashed outline box
                    drawRoundRect(
                        color = colors.border,
                        topLeft = Offset(0f, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    )

                    drawCircle(
                        color = colors.textTertiary,
                        radius = 2.5.dp.toPx(),
                        center = Offset(barWidth / 2f, targetY)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day of week label: Lun, Mar, Mié...
        Text(
            text = day.dayLabel,
            fontSize = 12.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (day.isToday) colors.textPrimary else colors.textSecondary
        )
    }
}
