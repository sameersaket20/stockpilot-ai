package com.stockpilot.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StockPilotColorScheme = darkColorScheme(
    primary = StockPilotTeal,
    onPrimary = StockPilotNavy,
    background = StockPilotNavy,
    onBackground = StockPilotTextPrimary,
    surface = StockPilotSurface,
    onSurface = StockPilotTextPrimary,
    surfaceVariant = StockPilotSurfaceElevated,
    onSurfaceVariant = StockPilotTextSecondary,
    error = StockPilotError,
    tertiary = StockPilotWarning,
    outline = Color(0xFF334155),
)

@Composable
fun StockPilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StockPilotColorScheme,
        typography = StockPilotTypography,
        content = content,
    )
}