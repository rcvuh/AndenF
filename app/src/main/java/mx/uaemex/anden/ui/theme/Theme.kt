package mx.uaemex.anden.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AndenBurgundyPrimary = Color(0xFF6B1D2F)
val AndenBurgundyDark = Color(0xFF4A101E)
val AndenGoldSecondary = Color(0xFFC59B27)
val AndenEmeraldValid = Color(0xFF1E7E34)
val AndenAmberWarning = Color(0xFFD97706)
val AndenDangerRed = Color(0xFFB91C1C)
val AndenSurfaceLight = Color(0xFFFBF8F6)
val AndenCardSurface = Color(0xFFFFFFFF)

private val AndenLightColorScheme = lightColorScheme(
    primary = AndenBurgundyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE8EC),
    onPrimaryContainer = AndenBurgundyDark,
    secondary = AndenGoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF4D6),
    onSecondaryContainer = Color(0xFF423000),
    tertiary = AndenEmeraldValid,
    onTertiary = Color.White,
    background = AndenSurfaceLight,
    onBackground = Color(0xFF1F1A1C),
    surface = AndenCardSurface,
    onSurface = Color(0xFF1F1A1C)
)

private val AndenHighContrastColorScheme = darkColorScheme(
    primary = Color(0xFFFFD600),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332B00),
    onPrimaryContainer = Color(0xFFFFEA70),
    secondary = Color(0xFF00E676),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003319),
    onSecondaryContainer = Color(0xFF80FFB8),
    tertiary = Color(0xFF40C4FF),
    onTertiary = Color.Black,
    background = Color(0xFF0A0A0A),
    onBackground = Color.White,
    surface = Color(0xFF141414),
    onSurface = Color.White
)

@Composable
fun AndenTheme(
    highContrastMode: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (highContrastMode) AndenHighContrastColorScheme else AndenLightColorScheme,
        content = content
    )
}
