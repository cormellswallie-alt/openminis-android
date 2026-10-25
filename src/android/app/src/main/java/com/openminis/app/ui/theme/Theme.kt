package com.openminis.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// [T-noir-redesign] Minis Noir colour schemes.
//
// Round 4 ("Spectra v3"): pure white / ink.
//
//   light  page #F7F7F8, card/chat #FFFFFF, sunk #F4F4F6, ink #0A0A0B
//   dark   page #0A0A0B, card #151517, raised #1C1C1F, text #F4F4F5
//
// `primary` is ink (ivory in dark): switches, buttons, progress are
// monochrome. The only hue in the chrome is the assistant orb.

private val NoirLight = lightColorScheme(
    primary = Noir.Ink,
    onPrimary = Color.White,
    primaryContainer = Noir.PaperSunk,
    onPrimaryContainer = Noir.Ink,
    inversePrimary = Noir.Ivory,
    secondary = Noir.InkSoft,
    onSecondary = Color.White,
    secondaryContainer = Noir.PaperSunk,
    onSecondaryContainer = Noir.Ink,
    tertiary = Noir.ThinkOnPaper,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1EEFF),
    onTertiaryContainer = Color(0xFF2A1C72),
    background = Noir.Paper,
    onBackground = Noir.Ink,
    surface = Noir.Paper,
    onSurface = Noir.Ink,
    surfaceVariant = Noir.PaperCard,
    onSurfaceVariant = Noir.InkMute,
    surfaceTint = Color.Transparent,
    inverseSurface = Noir.Ink,
    inverseOnSurface = Noir.Ivory,
    error = Noir.Err,
    onError = Color.White,
    errorContainer = Color(0xFFFDECEC),
    onErrorContainer = Color(0xFF6B1316),
    outline = Noir.PaperHairlineStrong,
    outlineVariant = Noir.PaperHairline,
    scrim = Color(0xFF000000),
    surfaceBright = Noir.PaperRaised,
    surfaceDim = Noir.PaperSunk,
    surfaceContainerLowest = Noir.Paper,
    surfaceContainerLow = Noir.PaperCard,
    surfaceContainer = Noir.PaperCard,
    surfaceContainerHigh = Noir.PaperRaised,
    surfaceContainerHighest = Noir.PaperRaised,
)

private val NoirDark = darkColorScheme(
    primary = Noir.Ivory,
    onPrimary = Noir.Onyx,
    primaryContainer = Noir.Onyx3,
    onPrimaryContainer = Noir.Ivory,
    inversePrimary = Noir.Ink,
    secondary = Noir.IvorySoft,
    onSecondary = Noir.Onyx,
    secondaryContainer = Noir.Onyx3,
    onSecondaryContainer = Noir.Ivory,
    tertiary = Noir.ThinkOnOnyx,
    onTertiary = Color(0xFF1B1240),
    tertiaryContainer = Color(0xFF2A2350),
    onTertiaryContainer = Color(0xFFE6DFFF),
    background = Noir.Onyx,
    onBackground = Noir.Ivory,
    surface = Noir.Onyx,
    onSurface = Noir.Ivory,
    surfaceVariant = Noir.Onyx1,
    onSurfaceVariant = Noir.IvoryMute,
    surfaceTint = Color.Transparent,
    inverseSurface = Noir.Ivory,
    inverseOnSurface = Noir.Onyx,
    error = Noir.ErrOnOnyx,
    onError = Noir.Onyx,
    errorContainer = Color(0xFF3A1517),
    onErrorContainer = Color(0xFFFFD6D7),
    outline = Noir.OnyxHairlineStrong,
    outlineVariant = Noir.OnyxHairline,
    scrim = Color(0xFF000000),
    surfaceBright = Noir.Onyx2,
    surfaceDim = Noir.Onyx,
    surfaceContainerLowest = Noir.Onyx,
    surfaceContainerLow = Noir.Onyx1,
    surfaceContainer = Noir.Onyx1,
    surfaceContainerHigh = Noir.Onyx2,
    surfaceContainerHighest = Noir.Onyx2,
)

// App-wide FAB accent. Reads from ChatPalette so it follows the in-app theme
// override (theme_mode pref), not android.isSystemInDarkTheme(), which only
// tracks the system setting.
@Composable
fun minisFabColor(): Color = LocalChatPalette.current.fabAccent

// Noir shape system — soft, continuous-looking corners. Larger than M3
// defaults so cards and sheets read as objects rather than panels.
private val MinisShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),   // DropdownMenu, Tooltip, OutlinedTextField default
    small = RoundedCornerShape(14.dp),        // Chip, TextField
    medium = RoundedCornerShape(20.dp),       // Card, Snackbar
    large = RoundedCornerShape(24.dp),        // NavigationDrawer
    extraLarge = RoundedCornerShape(28.dp),   // Dialog, BottomSheet
)

@Composable
fun MinisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) NoirDark else NoirLight
    val typography = scaledTypography(fontScale)
    val chatPalette = if (darkTheme) DarkChatPalette else LightChatPalette

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MinisShapes,
        typography = typography,
    ) {
        CompositionLocalProvider(LocalChatPalette provides chatPalette, content = content)
    }
}

private fun TextStyle.scale(factor: Float): TextStyle =
    if (factor == 1f) this else copy(fontSize = fontSize * factor)

/**
 * Noir typography. Inter for every UI role; Instrument Serif for the large
 * display/headline roles (screen titles, the home wordmark, empty states).
 * Sizes follow M3 so existing layouts keep their rhythm; weights and
 * tracking are tuned for Inter (titles tighter, labels slightly open).
 */
private fun noirTypography(): Typography {
    // [T-spectra-v3] MiSans for every role. Sizes stay on the M3 scale so
    // layouts keep their rhythm; weights are lifted one step for MiSans
    // (its Regular is 330) and large roles get tighter tracking.
    val b = Typography()
    fun TextStyle.ms(w: FontWeight? = null, track: Float? = null) = copy(
        fontFamily = SpectraSans,
        fontWeight = w ?: fontWeight,
        letterSpacing = track?.em ?: letterSpacing,
    )
    return Typography(
        displayLarge = b.displayLarge.ms(FontWeight.Bold, -0.025f),
        displayMedium = b.displayMedium.ms(FontWeight.Bold, -0.025f),
        displaySmall = b.displaySmall.ms(FontWeight.Bold, -0.02f),
        headlineLarge = b.headlineLarge.ms(FontWeight.Bold, -0.02f),
        headlineMedium = b.headlineMedium.ms(FontWeight.SemiBold, -0.015f),
        headlineSmall = b.headlineSmall.ms(FontWeight.SemiBold, -0.01f),
        titleLarge = b.titleLarge.ms(FontWeight.SemiBold, -0.01f),
        titleMedium = b.titleMedium.ms(FontWeight.SemiBold, 0f),
        titleSmall = b.titleSmall.ms(FontWeight.Medium, 0f),
        bodyLarge = b.bodyLarge.ms(FontWeight.Normal, 0f),
        bodyMedium = b.bodyMedium.ms(FontWeight.Normal, 0f),
        bodySmall = b.bodySmall.ms(FontWeight.Normal, 0f).copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = b.labelLarge.ms(FontWeight.Medium, 0f),
        labelMedium = b.labelMedium.ms(FontWeight.Medium, 0f).copy(fontSize = 12.5.sp),
        labelSmall = b.labelSmall.ms(FontWeight.Medium, 0.01f).copy(fontSize = 12.sp),
    )
}

private fun scaledTypography(factor: Float): Typography {
    val base = noirTypography()
    return Typography(
        displayLarge = base.displayLarge.scale(factor),
        displayMedium = base.displayMedium.scale(factor),
        displaySmall = base.displaySmall.scale(factor),
        headlineLarge = base.headlineLarge.scale(factor),
        headlineMedium = base.headlineMedium.scale(factor),
        headlineSmall = base.headlineSmall.scale(factor),
        titleLarge = base.titleLarge.scale(factor),
        titleMedium = base.titleMedium.scale(factor),
        titleSmall = base.titleSmall.scale(factor),
        bodyLarge = base.bodyLarge.scale(factor),
        bodyMedium = base.bodyMedium.scale(factor),
        bodySmall = base.bodySmall.scale(factor),
        labelLarge = base.labelLarge.scale(factor),
        labelMedium = base.labelMedium.scale(factor),
        labelSmall = base.labelSmall.scale(factor),
    )
}
