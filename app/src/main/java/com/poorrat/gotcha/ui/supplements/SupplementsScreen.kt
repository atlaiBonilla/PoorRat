package com.poorrat.gotcha.ui.supplements

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poorrat.gotcha.ui.nutrition.components.PoorRatBottomBar
import com.poorrat.gotcha.ui.supplements.components.PoorRatSupplementsTopBar
import com.poorrat.gotcha.ui.supplements.modals.AddSupplementBottomSheet
import com.poorrat.gotcha.ui.supplements.modals.AdjustAlarmsBottomSheet
import com.poorrat.gotcha.ui.supplements.modals.DailyStackBottomSheet
import com.poorrat.gotcha.ui.theme.PoorRatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementsScreen(
    viewModel: SupplementsViewModel,
    onNavigateToTab: (String) -> Unit,
    onToggleTheme: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = PoorRatTheme.colors

    val dailyStackSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addSupplementSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val adjustAlarmsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        containerColor = colors.background,
        topBar = {
            PoorRatSupplementsTopBar(
                onAddClick = { viewModel.openAddSupplementModal() },
                onToggleTheme = onToggleTheme
            )
        },
        bottomBar = {
            PoorRatBottomBar(
                currentTab = "Suplementos",
                onTabSelected = onNavigateToTab
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Stack Diario
            SupplementNavCard(
                icon = Icons.Outlined.Medication,
                title = "Stack Diario",
                subtitle = uiState.completionSubtitle,
                badgeText = "${uiState.completionPercentage}%",
                onClick = { viewModel.openDailyStackModal() },
                testTag = "nav_card_daily_stack"
            )

            // 2. Agregar Suplementos
            SupplementNavCard(
                icon = Icons.Default.Add,
                title = "Agregar Suplementos",
                subtitle = "Añadir productos, dosis y frecuencias a tu stack",
                badgeText = null,
                onClick = { viewModel.openAddSupplementModal() },
                testTag = "nav_card_add_supplements"
            )

            // 3. Ajustar Alarmas
            SupplementNavCard(
                icon = Icons.Outlined.NotificationsNone,
                title = "Ajustar Alarmas",
                subtitle = "Configurar recordatorios para cada toma",
                badgeText = uiState.activeAlarmsBadgeText,
                onClick = { viewModel.openAdjustAlarmsModal() },
                testTag = "nav_card_adjust_alarms"
            )
        }
    }

    // Modal 1: Stack Diario
    if (uiState.isDailyStackModalVisible) {
        DailyStackBottomSheet(
            sheetState = dailyStackSheetState,
            uiState = uiState,
            onDismiss = { viewModel.closeDailyStackModal() },
            onToggleTaken = { id, currentTaken -> viewModel.toggleTaken(id, currentTaken) },
            onDeleteSupplement = { id -> viewModel.deleteSupplement(id) }
        )
    }

    // Modal 2: Agregar Suplemento
    if (uiState.isAddSupplementModalVisible) {
        AddSupplementBottomSheet(
            sheetState = addSupplementSheetState,
            uiState = uiState,
            onDismiss = { viewModel.closeAddSupplementModal() },
            onNameChanged = { viewModel.onAddNameChanged(it) },
            onDosageChanged = { viewModel.onAddDosageChanged(it) },
            onIntakeMomentChanged = { viewModel.onAddIntakeMomentChanged(it) },
            onAlarmEnabledChanged = { viewModel.onAddAlarmEnabledChanged(it) },
            onAlarmTimeChanged = { h, m -> viewModel.onAddAlarmTimeChanged(h, m) },
            onSubmit = { viewModel.submitAddSupplement() }
        )
    }

    // Modal 3: Ajustar Alarmas y Avisos
    if (uiState.isAdjustAlarmsModalVisible) {
        AdjustAlarmsBottomSheet(
            sheetState = adjustAlarmsSheetState,
            uiState = uiState,
            onDismiss = { viewModel.closeAdjustAlarmsModal() },
            onUpdateAlarmConfig = { id, enabled, hour, minute ->
                viewModel.updateAlarmConfig(id, enabled, hour, minute)
            }
        )
    }
}

@Composable
private fun SupplementNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String?,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = PoorRatTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            if (badgeText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.badgeBackground)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.badgeText
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
