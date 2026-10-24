package com.poorrat.gotcha.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class PoorRatColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val divider: Color,
    val badgeBackground: Color,
    val badgeText: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val buttonPrimaryBg: Color,
    val buttonPrimaryText: Color,
    val buttonSecondaryBg: Color,
    val buttonSecondaryText: Color,
    val sheetBackground: Color,
    val sheetDragHandle: Color,
    val topBarBg: Color,
    val bottomBarBg: Color,
    val bottomBarDivider: Color,
    val navSelected: Color,
    val navUnselected: Color,
    val navBadgeBg: Color,
    val navBadgeText: Color,
    val progressTrack: Color,
    val progressFill: Color
) {
    val dragHandle: Color get() = sheetDragHandle
    val textMuted: Color get() = textTertiary
    val borderFocused: Color get() = textPrimary
}

val DarkPoorRatColors = PoorRatColors(
    isDark = true,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceCard = DarkSurfaceCard,
    surfaceElevated = DarkSurfaceElevated,
    border = DarkBorder,
    borderSubtle = DarkBorderSubtle,
    textPrimary = TextWhite,
    textSecondary = TextMutedDark,
    textTertiary = TextSubtleDark,
    accent = TextWhite,
    divider = DarkBorderSubtle,
    badgeBackground = DarkBadgeBg,
    badgeText = TextWhite,
    inputBackground = DarkInputBg,
    inputBorder = DarkInputBorder,
    buttonPrimaryBg = TextWhite,
    buttonPrimaryText = TrueBlack,
    buttonSecondaryBg = DarkGrey,
    buttonSecondaryText = TextMutedDark,
    sheetBackground = DarkSheetBg,
    sheetDragHandle = DarkDragHandle,
    topBarBg = DarkBackground,
    bottomBarBg = DarkBackground,
    bottomBarDivider = DarkBorderSubtle,
    navSelected = TextWhite,
    navUnselected = TextMutedLight,
    navBadgeBg = TextWhite,
    navBadgeText = TrueBlack,
    progressTrack = DarkGrey,
    progressFill = TextWhite
)

val LightPoorRatColors = PoorRatColors(
    isDark = false,
    background = LightBackground,
    surface = LightSurface,
    surfaceCard = LightSurfaceCard,
    surfaceElevated = LightSurfaceElevated,
    border = LightBorder,
    borderSubtle = LightBorderSubtle,
    textPrimary = TextDark,
    textSecondary = TextMutedLight,
    textTertiary = TextSubtleLight,
    accent = TextDark,
    divider = LightBorderSubtle,
    badgeBackground = LightBadgeBg,
    badgeText = TextDark,
    inputBackground = LightInputBg,
    inputBorder = LightInputBorder,
    buttonPrimaryBg = TextDark,
    buttonPrimaryText = TextWhite,
    buttonSecondaryBg = LightGrey,
    buttonSecondaryText = TextMutedLight,
    sheetBackground = LightSheetBg,
    sheetDragHandle = LightDragHandle,
    topBarBg = LightBackground,
    bottomBarBg = LightSurface,
    bottomBarDivider = LightBorderSubtle,
    navSelected = TextDark,
    navUnselected = TextMutedDark,
    navBadgeBg = TextDark,
    navBadgeText = TextWhite,
    progressTrack = BorderGrey,
    progressFill = TextDark
)

val LocalPoorRatColors = staticCompositionLocalOf { DarkPoorRatColors }

object PoorRatTheme {
    val colors: PoorRatColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPoorRatColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = TextWhite,
    onPrimary = TrueBlack,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSheetBg,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextMutedDark,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = TextDark,
    onPrimary = TextWhite,
    background = LightBackground,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextMutedLight,
    outline = LightBorder
)

@Composable
fun PoorRatTheme(
    darkTheme: Boolean = true, // Default to true matching the dark mode mockups
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val poorRatColors = if (darkTheme) DarkPoorRatColors else LightPoorRatColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalPoorRatColors provides poorRatColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}