package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryDark = Color(0xFF1E293B)
val SecondaryDark = Color(0xFF0F172A)
val AccentCyan = Color(0xFF06B6D4)
val BullishGreen = Color(0xFF22C55E)
val BearishRed = Color(0xFFEF4444)
val WarningYellow = Color(0xFFF59E0B)

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    secondary = Color(0xFF38BDF8),
    onSecondary = Color.Black,
    background = Color(0xFF0B0F19),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    error = BearishRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFF0369A1),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    error = BearishRed,
    onError = Color.White
)

@Composable
fun ChartMindTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
