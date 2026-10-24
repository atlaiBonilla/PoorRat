package com.poorrat.gotcha.ui.supplements.modals

import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.data.local.entity.SupplementEntity
import com.poorrat.gotcha.ui.supplements.SupplementsUiState
import com.poorrat.gotcha.ui.supplements.SupplementsViewModel
import com.poorrat.gotcha.ui.theme.PoorRatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustAlarmsBottomSheet(
    sheetState: SheetState,
    uiState: SupplementsUiState,
    onDismiss: () -> Unit,
    onUpdateAlarmConfig: (Long, Boolean, Int, Int) -> Unit
) {
    val colors = PoorRatTheme.colors
    val context = LocalContext.current

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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ajustar Alarmas y Avisos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Recordatorios de tomas diarias",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_adjust_alarms")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subtítulo de sección
            Text(
                text = "ALARMAS POR SUPLEMENTO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.supplements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay suplementos registrados para configurar alarmas.",
                        color = colors.textMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = uiState.supplements,
                        key = { it.id }
                    ) { supplement ->
                        AlarmSupplementCard(
                            supplement = supplement,
                            onToggleAlarm = { enabled ->
                                onUpdateAlarmConfig(
                                    supplement.id,
                                    enabled,
                                    supplement.alarmHour,
                                    supplement.alarmMinute
                                )
                            },
                            onPickTime = {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        onUpdateAlarmConfig(
                                            supplement.id,
                                            true, // Al elegir hora se asegura encendida
                                            hourOfDay,
                                            minute
                                        )
                                    },
                                    supplement.alarmHour,
                                    supplement.alarmMinute,
                                    false
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmSupplementCard(
    supplement: SupplementEntity,
    onToggleAlarm: (Boolean) -> Unit,
    onPickTime: () -> Unit
) {
    val colors = PoorRatTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm_item_${supplement.id}"),
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
            // Fila Superior: Ícono campana, nombre, dosis/momento y switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    tint = if (supplement.alarmEnabled) colors.textPrimary else colors.textMuted,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = supplement.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${supplement.dosage} · ${supplement.intakeMoment}",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                Switch(
                    checked = supplement.alarmEnabled,
                    onCheckedChange = onToggleAlarm,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.background,
                        checkedTrackColor = colors.textPrimary,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surfaceElevated,
                        uncheckedBorderColor = colors.border
                    ),
                    modifier = Modifier.testTag("switch_alarm_${supplement.id}")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fila Inferior: HORA DE LA ALARMA y selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HORA DE LA ALARMA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 0.5.sp
                )

                Card(
                    modifier = Modifier
                        .alpha(if (supplement.alarmEnabled) 1f else 0.4f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = supplement.alarmEnabled, onClick = onPickTime)
                        .testTag("time_picker_button_${supplement.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (supplement.alarmEnabled) colors.surfaceElevated else colors.surfaceCard
                    ),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SupplementsViewModel.formatTime12h(
                                supplement.alarmHour,
                                supplement.alarmMinute
                            ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
