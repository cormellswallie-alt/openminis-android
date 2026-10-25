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
// Round 3 ("Iris"): cool neutrals, one accent.
//
//   light  page #F6F7F9, card/chat #FFFFFF, sunk #EFF1F5, ink #0F1115
//   dark   page #0D0E12, card #16171D, raised #1E2028, text #ECEEF4
//   accent Iris — #4A47D9 on paper, #8E90FF on onyx; violet for thinking
//
// `primary` is the accent now (switches, buttons, links, progress), so the
// app carries one clear hue instead of the earlier ink/ivory monochrome
// with hand-placed gold. Contrast: IrisDeep on white 6.6:1, white on
// IrisDeep 6.6:1, IrisBright on onyx 6.4:1.

private val NoirLight = lightColorScheme(
    primary = Noir.IrisDeep,
    onPrimary = Color.White,
    primaryContainer = Noir.IrisSoft,
    onPrimaryContainer = Color(0xFF1E1B6B),
    inversePrimary = Noir.IrisBright,
    secondary = Noir.InkSoft,
    onSecondary = Noir.Paper,
    secondaryContainer = Noir.PaperSunk,
    onSecondaryContainer = Noir.Ink,
    tertiary = Noir.ThinkOnPaper,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1EBFF),
    onTertiaryContainer = Color(0xFF2E1868),
    background = Noir.Paper,
    onBackground = Noir.Ink,
    surface = Noir.Paper,
    onSurface = Noir.Ink,
    surfaceVariant = Noir.PaperCard,
    onSurfaceVariant = Noir.InkMute,
    surfaceTint = Color.Transparent,
    inverseSurface = Noir.Ink,
    inverseOnSurface = Noir.Ivory,
    error = Color(0xFFD93A44),
    onError = Color.White,
    errorContainer = Color(0xFFFDE4E5),
    onErrorContainer = Color(0xFF5A0D13),
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
    primary = Noir.IrisBright,
    onPrimary = Color(0xFF0E0F3A),
    primaryContainer = Noir.IrisSoftDark,
    onPrimaryContainer = Color(0xFFDCDDFF),
    inversePrimary = Noir.IrisDeep,
    secondary = Noir.IvorySoft,
    onSecondary = Noir.Onyx,
    secondaryContainer = Noir.Onyx3,
    onSecondaryContainer = Noir.Ivory,
    tertiary = Noir.ThinkOnOnyx,
    onTertiary = Color(0xFF1E0F45),
    tertiaryContainer = Color(0xFF2E2350),
    onTertiaryContainer = Color(0xFFE6DCFF),
    background = Noir.Onyx,
    onBackground = Noir.Ivory,
    surface = Noir.Onyx,
    onSurface = Noir.Ivory,
    surfaceVariant = Noir.Onyx1,
    onSurfaceVariant = Noir.IvoryMute,
    surfaceTint = Color.Transparent,
    inverseSurface = Noir.Ivory,
    inverseOnSurface = Noir.Onyx,
    error = Color(0xFFFF7A80),
    onError = Noir.Onyx,
    errorContainer = Color(0xFF3D171B),
    onErrorContainer = Color(0xFFFFD5D7),
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
    extraSmall = RoundedCornerShape(14.dp),   // DropdownMenu, Tooltip, OutlinedTextField default
    small = RoundedCornerShape(14.dp),        // Chip, TextField
    medium = RoundedCornerShape(22.dp),       // Card, Snackbar
    large = RoundedCornerShape(26.dp),        // NavigationDrawer
    extraLarge = RoundedCornerShape(32.dp),   // Dialog, BottomSheet
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
    val b = Typography()
    fun TextStyle.sans(w: FontWeight? = null, track: Float? = null) = copy(
        fontFamily = NoirSans,
        fontWeight = w ?: fontWeight,
        letterSpacing = track?.em ?: letterSpacing,
    )
    fun TextStyle.serif() = copy(fontFamily = NoirSerif, fontWeight = FontWeight.Normal, letterSpacing = (-0.01).em)
    return Typography(
        displayLarge = b.displayLarge.serif(),
        displayMedium = b.displayMedium.serif(),
        displaySmall = b.displaySmall.serif(),
        headlineLarge = b.headlineLarge.serif(),
        headlineMedium = b.headlineMedium.serif(),
        headlineSmall = b.headlineSmall.serif(),
        titleLarge = b.titleLarge.sans(FontWeight.SemiBold, -0.01f),
        titleMedium = b.titleMedium.sans(FontWeight.SemiBold, -0.005f),
        titleSmall = b.titleSmall.sans(FontWeight.SemiBold, 0f),
        bodyLarge = b.bodyLarge.sans(track = -0.005f),
        bodyMedium = b.bodyMedium.sans(track = 0f),
        bodySmall = b.bodySmall.sans(track = 0.005f),
        labelLarge = b.labelLarge.sans(FontWeight.Medium, 0.005f),
        labelMedium = b.labelMedium.sans(FontWeight.Medium, 0.02f),
        labelSmall = b.labelSmall.sans(FontWeight.Medium, 0.04f).copy(fontSize = 11.sp),
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
