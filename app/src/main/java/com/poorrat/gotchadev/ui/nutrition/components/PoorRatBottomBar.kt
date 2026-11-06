package com.poorrat.gotchadev.ui.nutrition.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotchadev.ui.theme.PoorRatTheme

@Composable
fun PoorRatBottomBar(
    currentTab: String = "Alimentación",
    onTabSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("poorrat_bottom_bar"),
        color = colors.bottomBarBg
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                color = colors.bottomBarDivider,
                thickness = 1.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "Alimentación",
                    icon = Icons.Outlined.Restaurant,
                    isSelected = currentTab == "Alimentación",
                    onClick = { onTabSelected("Alimentación") },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    label = "Peso",
                    icon = Icons.Outlined.Scale,
                    isSelected = currentTab == "Peso",
                    onClick = { onTabSelected("Peso") },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    label = "Entreno",
                    icon = Icons.Outlined.FitnessCenter,
                    isSelected = currentTab == "Entreno",
                    onClick = { onTabSelected("Entreno") },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    label = "Suplementos",
                    icon = Icons.Outlined.Medication,
                    isSelected = currentTab == "Suplementos",
                    onClick = { onTabSelected("Suplementos") },
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    label = "Tienda",
                    icon = Icons.Outlined.ShoppingBag,
                    badgeCount = 1,
                    isSelected = currentTab == "Tienda",
                    onClick = { onTabSelected("Tienda") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    val activeColor = colors.navSelected
    val inactiveColor = colors.navUnselected

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag("nav_tab_${label.lowercase()}")
    ) {
        if (badgeCount != null) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = colors.navBadgeBg,
                        contentColor = colors.navBadgeText
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) activeColor else inactiveColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 10.5.sp,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) activeColor else inactiveColor
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Selected indicator dot
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(colors.navSelected, CircleShape)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
