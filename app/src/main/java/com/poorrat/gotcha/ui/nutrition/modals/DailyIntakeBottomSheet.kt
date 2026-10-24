package com.poorrat.gotcha.ui.nutrition.modals

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.data.local.entity.DailyFoodLog
import com.poorrat.gotcha.data.local.entity.FoodItem
import com.poorrat.gotcha.ui.nutrition.NutritionUiState
import com.poorrat.gotcha.ui.theme.PoorRatTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyIntakeBottomSheet(
    uiState: NutritionUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectFood: (FoodItem) -> Unit,
    onClearSelectedFood: () -> Unit,
    onGramsChange: (String) -> Unit,
    onManualValuesChange: (String, String, String, String) -> Unit,
    onRegisterMeal: () -> Unit,
    onDeleteMeal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
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
        modifier = modifier.testTag("daily_intake_bottom_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
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
                            text = "Consumo Diario",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Desglose por comidas de hoy",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_daily_intake_modal")
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

            // Summary Card (CONSUMO TOTAL HOY vs META DIARIA)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_summary_card_modal"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = BorderStroke(1.dp, colors.border),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "CONSUMO TOTAL HOY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = numberFormat.format(uiState.todayCalories),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "kcal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "META DIARIA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = numberFormat.format(uiState.config.dailyCalorieTarget),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "kcal",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
            }

            // COMIDAS REGISTRADAS (N):
            item {
                Text(
                    text = "COMIDAS REGISTRADAS (${uiState.todayLogs.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (uiState.todayLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay comidas registradas hoy",
                            fontSize = 13.sp,
                            color = colors.textTertiary
                        )
                    }
                }
            } else {
                items(uiState.todayLogs, key = { it.id }) { log ->
                    MealLogItem(
                        log = log,
                        timeString = timeFormat.format(Date(log.timestampMillis)),
                        onDelete = { onDeleteMeal(log.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(18.dp))
            }

            // REGISTRAR ALIMENTO Section
            item {
                Text(
                    text = "REGISTRAR ALIMENTO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Search Input Field
                OutlinedTextField(
                    value = uiState.foodSearchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_food_input"),
                    placeholder = {
                        Text("Buscar alimento por nombre", color = colors.textSecondary, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary)
                    },
                    trailingIcon = {
                        if (uiState.foodSearchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                onSearchChange("")
                                onClearSelectedFood()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = colors.textSecondary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.border,
                        unfocusedBorderColor = colors.border,
                        focusedContainerColor = colors.inputBackground,
                        unfocusedContainerColor = colors.inputBackground,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Autocomplete suggestions
            if (uiState.selectedFoodItem == null && uiState.foodSearchQuery.isNotBlank() && uiState.catalogSearchResults.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            uiState.catalogSearchResults.take(4).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectFood(item) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${item.caloriesPer100g.roundToInt()} kcal / 100g",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                                HorizontalDivider(color = colors.divider)
                            }
                        }
                    }
                }
            }

            val grams = uiState.gramsInput.toFloatOrNull() ?: 100f
            val isKnownFood = uiState.selectedFoodItem != null

            if (isKnownFood) {
                val food = uiState.selectedFoodItem!!
                val factor = grams / 100f
                val totalCal = (food.caloriesPer100g * factor).roundToInt()
                val totalProt = food.proteinPer100g * factor
                val totalCarbs = food.carbsPer100g * factor
                val totalFat = food.fatPer100g * factor

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = food.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }
                            Text(
                                text = "${food.caloriesPer100g.roundToInt()} kcal / 100 g",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    GramsAndTotalRow(
                        gramsInput = uiState.gramsInput,
                        totalCalories = totalCal,
                        onGramsChange = onGramsChange
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    MacroSummaryCard(
                        cal = totalCal,
                        prot = totalProt,
                        carbs = totalCarbs,
                        fat = totalFat
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onRegisterMeal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("register_meal_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.buttonPrimaryBg,
                            contentColor = colors.buttonPrimaryText
                        )
                    ) {
                        Text(
                            text = "+ Registrar $totalCal kcal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (uiState.foodSearchQuery.isNotBlank() && uiState.catalogSearchResults.isEmpty()) {
                val manualCal100 = uiState.manualCaloriesPer100g.toFloatOrNull() ?: 0f
                val manualProt100 = uiState.manualProteinPer100g.toFloatOrNull() ?: 0f
                val manualCarbs100 = uiState.manualCarbsPer100g.toFloatOrNull() ?: 0f
                val manualFat100 = uiState.manualFatPer100g.toFloatOrNull() ?: 0f

                val factor = grams / 100f
                val totalCal = (manualCal100 * factor).roundToInt()
                val totalProt = manualProt100 * factor
                val totalCarbs = manualCarbs100 * factor
                val totalFat = manualFat100 * factor

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Alimento no encontrado",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Completa sus valores por 100 g para registrarlo en esta vista.",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SmallNumericInput(
                                    label = "Calorías / 100g",
                                    value = uiState.manualCaloriesPer100g,
                                    onValueChange = {
                                        onManualValuesChange(it, uiState.manualProteinPer100g, uiState.manualCarbsPer100g, uiState.manualFatPer100g)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                SmallNumericInput(
                                    label = "Proteína / 100g",
                                    value = uiState.manualProteinPer100g,
                                    onValueChange = {
                                        onManualValuesChange(uiState.manualCaloriesPer100g, it, uiState.manualCarbsPer100g, uiState.manualFatPer100g)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SmallNumericInput(
                                    label = "Carbos / 100g",
                                    value = uiState.manualCarbsPer100g,
                                    onValueChange = {
                                        onManualValuesChange(uiState.manualCaloriesPer100g, uiState.manualProteinPer100g, it, uiState.manualFatPer100g)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                SmallNumericInput(
                                    label = "Grasas / 100g",
                                    value = uiState.manualFatPer100g,
                                    onValueChange = {
                                        onManualValuesChange(uiState.manualCaloriesPer100g, uiState.manualProteinPer100g, uiState.manualCarbsPer100g, it)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    GramsAndTotalRow(
                        gramsInput = uiState.gramsInput,
                        totalCalories = totalCal,
                        onGramsChange = onGramsChange
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    MacroSummaryCard(
                        cal = totalCal,
                        prot = totalProt,
                        carbs = totalCarbs,
                        fat = totalFat
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    val canRegister = manualCal100 > 0f || manualProt100 > 0f || manualCarbs100 > 0f || manualFat100 > 0f

                    Button(
                        onClick = onRegisterMeal,
                        enabled = canRegister,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("register_custom_meal_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.buttonPrimaryBg,
                            contentColor = colors.buttonPrimaryText,
                            disabledContainerColor = colors.buttonSecondaryBg,
                            disabledContentColor = colors.buttonSecondaryText
                        )
                    ) {
                        Text(
                            text = "+ Registrar $totalCal kcal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Initial empty state when no search query
                item {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = colors.buttonSecondaryBg,
                            disabledContentColor = colors.buttonSecondaryText
                        )
                    ) {
                        Text("+ Registrar comida", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MealLogItem(
    log: DailyFoodLog,
    timeString: String,
    onDelete: () -> Unit
) {
    val colors = PoorRatTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meal_log_item_${log.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.foodName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeString,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = " · ",
                        fontSize = 12.sp,
                        color = colors.textTertiary
                    )
                    Text(
                        text = "${log.gramsConsumed.roundToInt()}g",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${log.calories} kcal",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_meal_${log.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GramsAndTotalRow(
    gramsInput: String,
    totalCalories: Int,
    onGramsChange: (String) -> Unit
) {
    val colors = PoorRatTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.inputBackground),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = gramsInput,
                    onValueChange = onGramsChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("grams_input_field"),
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
                        contentDescription = "Aumentar",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                val cur = gramsInput.toIntOrNull() ?: 100
                                onGramsChange((cur + 10).toString())
                            }
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Disminuir",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                val cur = gramsInput.toIntOrNull() ?: 100
                                if (cur > 10) onGramsChange((cur - 10).toString())
                            }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                Text(
                    text = "$totalCalories kcal",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun SmallNumericInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.inputBackground),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
                placeholder = { Text(label, fontSize = 11.sp, color = colors.textSecondary) },
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
        }
    }
}

@Composable
private fun MacroSummaryCard(
    cal: Int,
    prot: Float,
    carbs: Float,
    fat: Float
) {
    val colors = PoorRatTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MacroMiniColumn(label = "KCAL", value = "$cal")
            MacroMiniColumn(label = "PROTEÍNA", value = String.format(Locale.US, "%.1fg", prot))
            MacroMiniColumn(label = "CARBOS", value = String.format(Locale.US, "%.1fg", carbs))
            MacroMiniColumn(label = "GRASAS", value = String.format(Locale.US, "%.1fg", fat))
        }
    }
}

@Composable
private fun MacroMiniColumn(label: String, value: String) {
    val colors = PoorRatTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = colors.textSecondary
        )
    }
}
