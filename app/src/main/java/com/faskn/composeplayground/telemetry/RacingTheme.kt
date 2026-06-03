package com.faskn.composeplayground.telemetry

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.R
import com.faskn.composeplayground.ui.theme.Black900
import com.faskn.composeplayground.ui.theme.Gray400
import com.faskn.composeplayground.ui.theme.OrangeAccent
import com.faskn.composeplayground.ui.theme.White800

// ── Font families ──────────────────────────────────────────────────────────────

val MichromaFamily = FontFamily(
    Font(R.font.michroma_regular, FontWeight.Normal)
)

val OrbitronFamily = FontFamily(Font(R.font.orbitron))

val Exo2Family = FontFamily(Font(R.font.exo2))

// ── Racing Typography ──────────────────────────────────────────────────────────

const val FONT_FEATURE_TNUM = "tnum"

val RacingTypography = Typography(
    // Michroma
    headlineLarge  = TextStyle(fontFamily = MichromaFamily, fontWeight = FontWeight.Normal, fontSize = 32.sp, letterSpacing = 1.sp),
    headlineMedium = TextStyle(fontFamily = MichromaFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp, letterSpacing = 0.5.sp),
    headlineSmall  = TextStyle(fontFamily = MichromaFamily, fontWeight = FontWeight.Normal, fontSize = 20.sp, letterSpacing = 0.5.sp),

    // Orbitron
    titleLarge  = TextStyle(fontFamily = OrbitronFamily, fontWeight = FontWeight.Bold,   fontSize = 20.sp, letterSpacing = 0.sp, fontFeatureSettings = FONT_FEATURE_TNUM),
    titleMedium = TextStyle(fontFamily = OrbitronFamily, fontWeight = FontWeight.Bold,   fontSize = 16.sp, letterSpacing = 0.sp, fontFeatureSettings = FONT_FEATURE_TNUM),
    titleSmall  = TextStyle(fontFamily = OrbitronFamily, fontWeight = FontWeight.Bold,   fontSize = 14.sp, letterSpacing = 0.sp, fontFeatureSettings = FONT_FEATURE_TNUM),

    // Exo 2
    bodyLarge   = TextStyle(fontFamily = Exo2Family, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Normal, fontSize = 16.sp, fontFeatureSettings = FONT_FEATURE_TNUM),
    bodyMedium  = TextStyle(fontFamily = Exo2Family, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Normal, fontSize = 14.sp, fontFeatureSettings = FONT_FEATURE_TNUM),
    bodySmall   = TextStyle(fontFamily = Exo2Family, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Normal, fontSize = 12.sp, fontFeatureSettings = FONT_FEATURE_TNUM),
    labelLarge  = TextStyle(fontFamily = Exo2Family, fontWeight = FontWeight.Light, fontStyle = FontStyle.Normal, fontSize = 14.sp, fontFeatureSettings = FONT_FEATURE_TNUM),
    labelMedium = TextStyle(fontFamily = Exo2Family, fontWeight = FontWeight.Light, fontStyle = FontStyle.Normal, fontSize = 12.sp, fontFeatureSettings = FONT_FEATURE_TNUM),
    labelSmall  = TextStyle(fontFamily = Exo2Family, fontWeight = FontWeight.Light, fontStyle = FontStyle.Normal, fontSize = 10.sp, fontFeatureSettings = FONT_FEATURE_TNUM),
)

// ── Color scheme ───────────────────────────────────────────────────────────────

private val RacingColorScheme = darkColorScheme(
    primary        = OrangeAccent,
    onPrimary      = Black900,
    background     = Black900,
    onBackground   = White800,
    surface        = Black900,
    onSurface      = White800,
    primaryContainer    = OrangeAccent,
    onPrimaryContainer  = Black900,
)

data class RacingColors(
    val fastest: Color = Color(0xFF63B422),
    val slowest: Color = Color(0xFFB42238),
    val neutral: Color = Color.White,
    val accent: Color = OrangeAccent,
    val muted: Color = Gray400,
    val userDriver: Color = Color(0xFF6EE72D),
    val rivalDriver: Color = Color(0xFFE663C3),
    val rivalIndicator: Color = Color(0xFFE6DFED),
    val trackAsphalt: Color = Color(0xFF222222),
    val trackEdge: Color = Color.Red,
    val trackScratch: Color = Color.White.copy(alpha = 0.5f),
    val trackGlow: Color = Color(0xFFB91414).copy(alpha = 0.3f),
    val white: Color = Color.White,
    val black: Color = Color.Black,
    val red: Color = Color.Red,
    val transparentGrey: Color = Color(0xFF1C1C1C).copy(alpha = 0.8f),
    val telemetryBrake: Color = Color(0xFFB91414),
    val telemetryThrottle: Color = Color(0xFF00C853),
    val telemetryIdle: Color = Color(0xFFFFD600),
    val divider: Color = Color(0xFF262626),
    val raceEngineerSoft: Color = Color(0xFF6695EF),
    val raceEngineerDark: Color = Color(0xFF217BFE)
)

val LocalRacingColors = staticCompositionLocalOf { RacingColors() }

val racingColors: RacingColors
    @Composable get() = LocalRacingColors.current

// ── Theme entry point ──────────────────────────────────────────────────────────

@Composable
fun RacingTheme(
    colors: RacingColors = RacingColors(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalRacingColors provides colors) {
        MaterialTheme(
            colorScheme = RacingColorScheme,
            typography  = RacingTypography,
            content     = content
        )
    }
}
