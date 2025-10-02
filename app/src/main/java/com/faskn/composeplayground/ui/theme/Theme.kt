package com.faskn.composeplayground.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PremiumDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    secondary = Color(0xFFB0B0B0),
    tertiary = Color(0xFF232526),
    background = Color(0xFF181818),
    surface = Color(0xFF232526),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
)

@Composable
fun ComposePlaygroundTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PremiumDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}