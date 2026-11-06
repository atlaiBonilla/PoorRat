package com.poorrat.gotchadev.ui.nutrition.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotchadev.data.local.entity.DailyFoodLog
import com.poorrat.gotchadev.data.local.entity.FoodItem
import com.poorrat.gotchadev.ui.nutrition.NutritionUiState
import com.poorrat.gotchadev.ui.theme.PoorRatTheme
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
    onTabChange: (Int) -> Unit,
    onAiPromptChange: (String) -> Unit,
    onAiEstimate: () -> Unit,
    onAiRegister: () -> Unit,
    onAiUpdateValues: (String, Float, Int, Float, Float, Float) -> Unit,
    onManualFormChange: (name: String, grams: String, cal: String, prot: String, carbs: String, fat: String) -> Unit,
    onManualRegister: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectFood: (FoodItem) -> Unit,
    onClearSelectedFood: () -> Unit,
    onGramsChange: (String) -> Unit,
    onManualValuesChange: (String, String, String, String) -> Unit,
    onRegisterMeal: () -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onUseTypedQueryAsFood: ((String) -> Unit)? = null,
    onSwitchToAiWithQuery: ((String) -> Unit)? = null,
    onSwitchToManualWithQuery: ((String) -> Unit)? = null,
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(top = 28.dp, bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(colors.sheetDragHandle)
                )
            }
        },
        modifier = modifier.testTag("daily_intake_bottom_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 40.dp)
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
                                val targetStr = if (uiState.config.dailyCalorieTarget > 0) {
                                    numberFormat.format(uiState.config.dailyCalorieTarget)
                                } else {
                                    "--"
                                }
                                Text(
                                    text = targetStr,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.textPrimary
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

                Spacer(modifier = Modifier.height(20.dp))
            }

            // COMIDAS REGISTRADAS (N)
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

            // REGISTRAR ALIMENTO HEADER & TABS
            item {
                Text(
                    text = "AGREGAR ALIMENTO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Mode Selector Segmented Tabs (Catálogo como 1a opción, Estimar con IA, Ingreso Manual)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceCard)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AddFoodTabButton(
                        title = "Catálogo",
                        icon = Icons.Default.MenuBook,
                        isSelected = uiState.activeAddFoodTab == 0,
                        onClick = { onTabChange(0) },
                        modifier = Modifier.weight(1f)
                    )
                    AddFoodTabButton(
                        title = "Con IA",
                        icon = Icons.Default.AutoAwesome,
                        isSelected = uiState.activeAddFoodTab == 1,
                        onClick = { onTabChange(1) },
                        modifier = Modifier.weight(1f)
                    )
                    AddFoodTabButton(
                        title = "Manual",
                        icon = Icons.Default.Edit,
                        isSelected = uiState.activeAddFoodTab == 2,
                        onClick = { onTabChange(2) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // TAB 0: BUSCAR EN CATÁLOGO (PREDETERMINADO)
            if (uiState.activeAddFoodTab == 0) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Search Input Field
                        OutlinedTextField(
                            value = uiState.foodSearchQuery,
                            onValueChange = onSearchChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_food_input"),
                            placeholder = {
                                Text("Buscar en catálogo o escribir alimento...", color = colors.textSecondary.copy(alpha = 0.5f), fontSize = 14.sp)
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
                                unfocusedTextColor = colors.textPrimary,
                                cursorColor = colors.textPrimary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.foodSearchQuery.isBlank())
                                "${uiState.catalogSearchResults.size} alimentos en el catálogo (escribe para buscar)"
                            else
                                "${uiState.catalogSearchResults.size} resultados encontrados",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Food Items list from catalog
                if (uiState.selectedFoodItem == null) {
                    // Quick option: Use typed query directly as food item if not blank
                    if (uiState.foodSearchQuery.isNotBlank()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .clickable {
                                        onUseTypedQueryAsFood?.invoke(uiState.foodSearchQuery)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Usar \"${uiState.foodSearchQuery.trim()}\"",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Ingresar cantidad y registrar directamente",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.catalogSearchResults.isNotEmpty()) {
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
                                    uiState.catalogSearchResults.take(40).forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelectFood(item) }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = "P: ${item.proteinPer100g}g • C: ${item.carbsPer100g}g • G: ${item.fatPer100g}g",
                                                    fontSize = 11.sp,
                                                    color = colors.textSecondary
                                                )
                                            }
                                            Text(
                                                text = "${item.caloriesPer100g.roundToInt()} kcal / 100g",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textSecondary
                                            )
                                        }
                                        HorizontalDivider(color = colors.divider)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (uiState.foodSearchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (uiState.foodSearchQuery.isNotBlank())
                                            "No se encontraron alimentos para \"${uiState.foodSearchQuery}\""
                                        else
                                            "El catálogo está vacío. Agrega documentos a la colección 'food_catalog' en Firebase Firestore para que aparezcan aquí automáticamente.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    if (uiState.foodSearchQuery.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "¿Deseas registrar este alimento ahora?",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = { onUseTypedQueryAsFood?.invoke(uiState.foodSearchQuery) },
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.buttonPrimaryBg,
                                                contentColor = colors.buttonPrimaryText
                                            )
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Ingresar \"${uiState.foodSearchQuery.trim()}\"", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onSwitchToAiWithQuery?.invoke(uiState.foodSearchQuery) ?: onTabChange(1) },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, colors.border)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.textPrimary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Con IA", fontSize = 11.sp, color = colors.textPrimary)
                                            }
                                            OutlinedButton(
                                                onClick = { onSwitchToManualWithQuery?.invoke(uiState.foodSearchQuery) ?: onTabChange(2) },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, colors.border)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.textPrimary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Manual", fontSize = 11.sp, color = colors.textPrimary)
                                            }
                                        }
                                    }
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = food.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "${food.caloriesPer100g.roundToInt()} kcal / 100 g",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                                IconButton(onClick = onClearSelectedFood) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cambiar alimento",
                                        tint = colors.textSecondary
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick portion selector chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(50, 100, 150, 200, 300).forEach { portion ->
                                val isSelected = uiState.gramsInput == portion.toString()
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) colors.buttonPrimaryBg else colors.surfaceCard,
                                    border = BorderStroke(1.dp, if (isSelected) colors.buttonPrimaryBg else colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onGramsChange(portion.toString()) }
                                ) {
                                    Text(
                                        text = "${portion}g",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.buttonPrimaryText else colors.textSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }

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
                }
            }

            // TAB 1: ESTIMAR CON IA
            if (uiState.activeAddFoodTab == 1) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Describe lo que comiste en lenguaje natural:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = uiState.aiFoodPrompt,
                            onValueChange = onAiPromptChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_food_prompt_input"),
                            placeholder = {
                                Text(
                                    "Ej. 2 huevos revueltos con 2 rebanadas de pan tostado y un café con leche",
                                    color = colors.textSecondary.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.border,
                                unfocusedBorderColor = colors.border,
                                focusedContainerColor = colors.inputBackground,
                                unfocusedContainerColor = colors.inputBackground,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )

                        if (uiState.aiErrorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                                border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.aiErrorMessage,
                                        color = Color(0xFFEF5350),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onAiEstimate,
                            enabled = !uiState.isAiEstimating && uiState.aiFoodPrompt.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ai_estimate_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.buttonPrimaryBg,
                                contentColor = colors.buttonPrimaryText,
                                disabledContainerColor = colors.buttonSecondaryBg,
                                disabledContentColor = colors.buttonSecondaryText
                            )
                        ) {
                            if (uiState.isAiEstimating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = colors.buttonPrimaryText,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Consultando Google Search e IA...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buscar Macros con Google Search", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        val result = uiState.aiEstimatedResult
                        if (result != null) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_result_card"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = result.foodName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = "Porción aprox: ${result.grams.roundToInt()} g",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                        }

                                        Text(
                                            text = "${result.calories} kcal",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colors.textPrimary
                                        )
                                    }

                                    if (result.summaryDescription.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.surfaceCard)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = result.summaryDescription,
                                                fontSize = 11.sp,
                                                color = colors.textSecondary,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Macro breakdown
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        MacroBadge(label = "PROTEÍNA", value = "${result.protein}g", modifier = Modifier.weight(1f))
                                        MacroBadge(label = "CARBOS", value = "${result.carbs}g", modifier = Modifier.weight(1f))
                                        MacroBadge(label = "GRASAS", value = "${result.fat}g", modifier = Modifier.weight(1f))
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = onAiRegister,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .testTag("register_ai_food_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.buttonPrimaryBg,
                                            contentColor = colors.buttonPrimaryText
                                        )
                                    ) {
                                        Text(
                                            text = "+ Registrar ${result.calories} kcal",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: INGRESO MANUAL EXACTO
            if (uiState.activeAddFoodTab == 2) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Ingresa los datos exactos que conozcas. Cualquier campo vacío se registrará en 0:",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Food Name
                        Text(
                            text = "NOMBRE DEL ALIMENTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.manualFoodName,
                            onValueChange = {
                                onManualFormChange(
                                    it,
                                    uiState.manualGrams,
                                    uiState.manualCaloriesTotal,
                                    uiState.manualProteinTotal,
                                    uiState.manualCarbsTotal,
                                    uiState.manualFatTotal
                                )
                            },
                            placeholder = { Text("Ej. Ensalada César o Yogur Griego", color = colors.textSecondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_food_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.border,
                                unfocusedBorderColor = colors.border,
                                focusedContainerColor = colors.inputBackground,
                                unfocusedContainerColor = colors.inputBackground,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grams / Portion
                        Text(
                            text = "GRAMOS / PORCIÓN (g)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.manualGrams,
                            onValueChange = {
                                onManualFormChange(
                                    uiState.manualFoodName,
                                    it,
                                    uiState.manualCaloriesTotal,
                                    uiState.manualProteinTotal,
                                    uiState.manualCarbsTotal,
                                    uiState.manualFatTotal
                                )
                            },
                            placeholder = { Text("Gramos (ej. 150)", color = colors.textSecondary.copy(alpha = 0.5f), fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_food_grams_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.border,
                                unfocusedBorderColor = colors.border,
                                focusedContainerColor = colors.inputBackground,
                                unfocusedContainerColor = colors.inputBackground,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4 Numeric Inputs Row (Kcal, Prot, Carbs, Fat)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ManualFieldBox(
                                label = "Kcal (0)",
                                value = uiState.manualCaloriesTotal,
                                onValueChange = {
                                    onManualFormChange(
                                        uiState.manualFoodName,
                                        uiState.manualGrams,
                                        it,
                                        uiState.manualProteinTotal,
                                        uiState.manualCarbsTotal,
                                        uiState.manualFatTotal
                                    )
                                },
                                isDecimal = false,
                                modifier = Modifier.weight(1f)
                            )
                            ManualFieldBox(
                                label = "Prot g (0)",
                                value = uiState.manualProteinTotal,
                                onValueChange = {
                                    onManualFormChange(
                                        uiState.manualFoodName,
                                        uiState.manualGrams,
                                        uiState.manualCaloriesTotal,
                                        it,
                                        uiState.manualCarbsTotal,
                                        uiState.manualFatTotal
                                    )
                                },
                                isDecimal = true,
                                modifier = Modifier.weight(1f)
                            )
                            ManualFieldBox(
                                label = "Carbs g (0)",
                                value = uiState.manualCarbsTotal,
                                onValueChange = {
                                    onManualFormChange(
                                        uiState.manualFoodName,
                                        uiState.manualGrams,
                                        uiState.manualCaloriesTotal,
                                        uiState.manualProteinTotal,
                                        it,
                                        uiState.manualFatTotal
                                    )
                                },
                                isDecimal = true,
                                modifier = Modifier.weight(1f)
                            )
                            ManualFieldBox(
                                label = "Grasas g (0)",
                                value = uiState.manualFatTotal,
                                onValueChange = {
                                    onManualFormChange(
                                        uiState.manualFoodName,
                                        uiState.manualGrams,
                                        uiState.manualCaloriesTotal,
                                        uiState.manualProteinTotal,
                                        uiState.manualCarbsTotal,
                                        it
                                    )
                                },
                                isDecimal = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onManualRegister,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("register_manual_food_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.buttonPrimaryBg,
                                contentColor = colors.buttonPrimaryText
                            )
                        ) {
                            val c = uiState.manualCaloriesTotal.toIntOrNull() ?: 0
                            Text(
                                text = if (c > 0) "+ Registrar $c kcal" else "+ Registrar Alimento",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFoodTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    val isDark = colors.isDark

    val bg = if (isSelected) {
        if (isDark) Color.White else Color(0xFF111111)
    } else Color.Transparent

    val textCol = if (isSelected) {
        if (isDark) Color.Black else Color.White
    } else colors.textSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textCol,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textCol,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ManualFieldBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDecimal: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = colors.inputBackground),
            border = BorderStroke(1.dp, colors.border)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("", color = colors.textSecondary.copy(alpha = 0.4f), fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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
private fun MacroBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
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
        // Grams Input Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "CANTIDAD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = gramsInput,
                            onValueChange = onGramsChange,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            singleLine = true,
                            modifier = Modifier
                                .width(85.dp)
                                .testTag("grams_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.border,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedContainerColor = colors.inputBackground,
                                unfocusedContainerColor = colors.inputBackground,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                cursorColor = colors.textPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "g", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                    }
                }

                // Increment / Decrement
                Column {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Aumentar",
                        tint = colors.textPrimary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                val cur = gramsInput.toIntOrNull() ?: 100
                                onGramsChange((cur + 10).toString())
                            }
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Disminuir",
                        tint = colors.textPrimary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                val cur = gramsInput.toIntOrNull() ?: 100
                                if (cur > 10) onGramsChange((cur - 10).toString())
                            }
                    )
                }
            }
        }

        // Total Calories Display Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(text = "TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$totalCalories",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "kcal",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MacroItem(label = "PROTEÍNA", grams = String.format(Locale.US, "%.1fg", prot))
            MacroItem(label = "CARBOS", grams = String.format(Locale.US, "%.1fg", carbs))
            MacroItem(label = "GRASAS", grams = String.format(Locale.US, "%.1fg", fat))
        }
    }
}

@Composable
private fun MacroItem(label: String, grams: String) {
    val colors = PoorRatTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = grams, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
    }
}
