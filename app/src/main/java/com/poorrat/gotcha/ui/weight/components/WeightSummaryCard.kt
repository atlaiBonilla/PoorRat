package com.poorrat.gotcha.ui.weight.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.data.preferences.WeightUnit
import com.poorrat.gotcha.ui.theme.PoorRatTheme
import java.util.Locale

@Composable
fun WeightSummaryCard(
    currentWeightKg: Float,
    targetWeightKg: Float,
    differenceKg: Float,
    unit: WeightUnit = WeightUnit.KG,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors

    val displayCurrent = if (unit == WeightUnit.LB) currentWeightKg * 2.20462f else currentWeightKg
    val displayTarget = if (unit == WeightUnit.LB) targetWeightKg * 2.20462f else targetWeightKg
    val displayDiff = if (unit == WeightUnit.LB) differenceKg * 2.20462f else differenceKg
    val unitSymbol = unit.symbol

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weight_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 22.dp, horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: PESO ACTUAL
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = "PESO ACTUAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f", displayCurrent),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unitSymbol,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val diffFormatted = if (displayDiff > 0f) {
                    "+${String.format(Locale.US, "%.1f", displayDiff)} $unitSymbol meta"
                } else {
                    "${String.format(Locale.US, "%.1f", displayDiff)} $unitSymbol meta"
                }
                Text(
                    text = diffFormatted,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            // Subtle vertical separator line
            Surface(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                color = colors.divider
            ) {}

            // Right Column: PESO META
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = "PESO META",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f", displayTarget),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unitSymbol,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Objetivo definido",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}
