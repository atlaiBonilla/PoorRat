package com.poorrat.gotcha.ui.workout.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.data.local.entity.RoutineDayEntity
import com.poorrat.gotcha.ui.theme.PoorRatTheme

@Composable
fun WorkoutDayHeader(
    dayEntity: RoutineDayEntity?,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    val label = dayEntity?.dayLabel?.uppercase() ?: "DÍA 1 · LUNES"
    val routineName = dayEntity?.routineName ?: "Upper de fuerza"
    val bodyPart = dayEntity?.bodyPart ?: "Upper"

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DÍA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary,
            letterSpacing = 1.sp,
            modifier = Modifier.testTag("workout_current_day_title")
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$routineName · $bodyPart · ~50 min",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textSecondary,
            modifier = Modifier.testTag("workout_current_day_subtitle")
        )
    }
}

@Composable
fun MainEntrenaCard(
    isRestDay: Boolean,
    exerciseCount: Int,
    isSessionRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    val isDark = colors.isDark

    val containerBg = if (isRestDay) {
        if (isDark) Color(0xFF1E1E22) else Color(0xFFE4E4E7)
    } else {
        if (isDark) Color(0xFF161619) else Color(0xFF000000)
    }

    val contentTextColor = if (isRestDay) colors.textSecondary else Color.White
    val subTextColor = if (isRestDay) colors.textSecondary else Color(0xFFCCCCCC)
    val cardBorder = if (isDark) BorderStroke(1.dp, Color(0xFF2C2C32)) else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = !isRestDay, onClick = onClick)
            .testTag("main_entrena_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Row with Play Circle and ENTRENA
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (isRestDay) colors.textSecondary else Color.White,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Entrenar",
                        tint = if (isRestDay) colors.background else Color(0xFF111111),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = if (isSessionRunning) "EN CURSO" else "ENTRENA",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentTextColor,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics row matching Image 5
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isRestDay) {
                    Text(
                        text = "Día de descanso programado",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = subTextColor
                    )
                } else {
                    // Metric 1: Ejercicios
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "$exerciseCount Ejercicios",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = subTextColor
                    )

                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "·", fontSize = 14.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.width(10.dp))

                    // Metric 2: Minutos
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "50 min",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = subTextColor
                    )

                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "·", fontSize = 14.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.width(10.dp))

                    // Metric 3: Calorías
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "~420 kcal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = subTextColor
                    )
                }
            }
        }
    }
}
