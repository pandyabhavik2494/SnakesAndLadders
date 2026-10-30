package com.pandyabhavik.snakes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pandyabhavik.snakes.R
import com.pandyabhavik.snakes.ui.Theme

/** Playfair Display for the name and the big moments; Manrope for everything you read in passing. Both bundled. */
val Display = FontFamily(
    Font(R.font.playfair, FontWeight.Normal),
    Font(R.font.playfair, FontWeight.Medium),
    Font(R.font.playfair, FontWeight.SemiBold),
    Font(R.font.playfair, FontWeight.Bold),
)
val Ui = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold),
)

private val typography = Typography(
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 50.sp),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = 0.3.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.3.sp),
    titleMedium = TextStyle(fontFamily = Ui, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontFamily = Ui, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Ui, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Ui, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = Ui, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 2.sp),
)

@Composable
fun SnakesTheme(theme: Theme, content: @Composable () -> Unit) {
    val scheme = if (theme.light) {
        lightColorScheme(primary = theme.accent, onPrimary = theme.onAccent, background = theme.wallCentre, onBackground = theme.ink, surface = theme.surface, onSurface = theme.ink, onSurfaceVariant = theme.dim, outline = theme.frame)
    } else {
        darkColorScheme(primary = theme.accent, onPrimary = theme.onAccent, background = theme.wallCentre, onBackground = theme.ink, surface = theme.surface, onSurface = theme.ink, onSurfaceVariant = theme.dim, outline = theme.frame)
    }
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
