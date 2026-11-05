package com.poorrat.gotcha.ui.workout.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
fun PoorRatWorkoutTopBar(
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
            .displayCutoutPadding()
            .padding(top = 18.dp, bottom = 10.dp)
    ) {
        // Logo on left (White negative in dark mode, black in light mode, transparent PNG)
        val logoResId = if (isDark) R.drawable.logo_poorrat_dark else R.drawable.logo_poorrat_light
        Image(
            painter = painterResource(id = logoResId),
            contentDescription = "PoorRat Logo - Ajustes y Perfil",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .height(32.dp)
                .testTag("poorrat_workout_logo")
                .then(if (onLogoClick != null) Modifier.clickable { onLogoClick() } else Modifier),
            contentScale = ContentScale.Fit
        )

        // Title strictly centered to the entire screen width
        Text(
            text = "ENTRENAMIENTO",
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
                .testTag("workout_top_bar_title")
        )

        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(24.dp)
                .testTag("workout_top_bar_icon")
        )
    }
}
