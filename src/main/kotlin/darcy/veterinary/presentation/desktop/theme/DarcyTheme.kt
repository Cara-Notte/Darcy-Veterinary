package darcy.veterinary.presentation.desktop.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object DarcyColor {
    val Zinc950 = Color(0xFF09090B)
    val Zinc900 = Color(0xFF18181B)
    val Zinc850 = Color(0xFF202024)
    val Zinc800 = Color(0xFF27272A)
    val Zinc700 = Color(0xFF3F3F46)
    val Zinc400 = Color(0xFFA1A1AA)
    val Zinc300 = Color(0xFFD4D4D8)
    val Zinc100 = Color(0xFFF4F4F5)

    val Slate950 = Color(0xFF020617)
    val Slate900 = Color(0xFF0F172A)

    val ClinicalAmber = Color(0xFFF59E0B)
    val ClinicalAmberPressed = Color(0xFFD97706)
    val SemanticRed = Color(0xFFEF4444)

    val AppBackground = Zinc950
    val GlassSurface = Color(0xCC18181B)
    val GlassSurfaceStrong = Color(0xE6202024)
    val GlassSurfaceSubtle = Color(0x9927272A)
    val GlassBorder = Color(0x2EFFFFFF)
    val GlassBorderStrong = Color(0x4DFFFFFF)

    val TextPrimary = Zinc100
    val TextSecondary = Zinc300
    val TextMuted = Zinc400
}

private val DarcyMaterialColors = darkColors(
    primary = DarcyColor.ClinicalAmber,
    primaryVariant = DarcyColor.ClinicalAmberPressed,
    secondary = DarcyColor.ClinicalAmber,
    background = DarcyColor.AppBackground,
    surface = DarcyColor.GlassSurfaceStrong,
    error = DarcyColor.SemanticRed,
    onPrimary = DarcyColor.Zinc950,
    onSecondary = DarcyColor.Zinc950,
    onBackground = DarcyColor.TextPrimary,
    onSurface = DarcyColor.TextPrimary,
    onError = DarcyColor.Zinc950
)

@Composable
fun DarcyVetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = DarcyMaterialColors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
