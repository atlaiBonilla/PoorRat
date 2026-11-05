package com.poorrat.gotcha.ui.nutrition.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poorrat.gotcha.R
import com.poorrat.gotcha.ui.theme.PoorRatTheme

@Composable
fun PoorRatTopBar(
    onAddClick: () -> Unit,
    onToggleTheme: (() -> Unit)? = null,
    onLogoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = PoorRatTheme.colors
    val isDark = colors.isDark

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 18.dp, bottom = 10.dp)
    ) {
        // Logo on left (Negative white in dark mode, black in light mode, transparent PNG)
        val logoResId = if (isDark) R.drawable.logo_poorrat_dark else R.drawable.logo_poorrat_light
        Image(
            painter = painterResource(id = logoResId),
            contentDescription = "PoorRat Logo - Ajustes y Perfil",
            modifier = Modifier
                .padding(start = 20.dp)
                .height(32.dp)
                .align(Alignment.CenterStart)
                .testTag("poorrat_logo")
                .then(if (onLogoClick != null) Modifier.clickable { onLogoClick() } else Modifier),
            contentScale = ContentScale.Fit
        )

        // Title Centered: ALIMENTACIÓN (centrado exactamente a la pantalla)
        Text(
            text = "ALIMENTACIÓN",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = colors.textPrimary,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 76.dp)
                .align(Alignment.Center)
                .testTag("top_bar_title")
        )

        // Plus icon button on right
        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(40.dp)
                .align(Alignment.CenterEnd)
                .testTag("add_meal_button"),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = colors.textPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Registrar Alimento",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
