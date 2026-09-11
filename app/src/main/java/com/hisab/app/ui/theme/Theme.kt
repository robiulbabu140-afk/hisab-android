package com.hisab.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = HisabPurple,
    onPrimary = HisabCard,
    primaryContainer = HisabPurpleLight,
    onPrimaryContainer = HisabPurple,
    background = HisabBg,
    onBackground = HisabInk,
    surface = HisabCard,
    onSurface = HisabInk,
    surfaceVariant = HisabLine,
    onSurfaceVariant = HisabMuted,
    error = HisabRed
)

private val DarkColors = darkColorScheme(
    primary = HisabPurpleLight,
    onPrimary = HisabPurpleDark,
    primaryContainer = HisabPurpleDark,
    onPrimaryContainer = HisabPurpleLight,
    error = HisabRed
)

private val HisabTypography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 14.sp),
    bodyMedium = TextStyle(fontSize = 13.sp),
    bodySmall = TextStyle(fontSize = 11.sp),
    labelSmall = TextStyle(fontSize = 10.sp)
)

@Composable
fun HisabTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HisabTypography,
        content = content
    )
}
