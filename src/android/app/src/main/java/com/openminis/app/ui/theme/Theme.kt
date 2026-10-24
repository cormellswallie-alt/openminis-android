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
// Replaces the desaturated iOS-blue accent (#528AD2 / #6A94CE) and the iOS
// grouped greys (#F2F2F7 / #000 / #1C1C1E). The scheme is near-monochrome:
//
//   light  page Paper #F3F0E9, card #FBFAF6, raised #FFFFFF, ink #141413
//   dark   page Onyx  #0B0B0C, card #151516, raised #1D1D1F, ivory #EFEBE3
//
// `primary` is INK in light mode and IVORY in dark mode, not a hue: primary
// buttons, switches and the send button read as solid ink/ivory, the way
// the design boards render them. Champagne gold is NOT a Material role — it
// is applied by hand (Noir.goldBrush) to the few places that earn it, so it
// can never leak into 1000+ colorScheme call sites as a loud accent.
//
// Contrast (WCAG): ink on paper 16.9:1, ivory on onyx 16.2:1, InkMute on
// paper 3.2:1 and IvoryMute on onyx 4.9:1 — the mute tones are only used
// for secondary/tertiary text at ≥ 12sp, matching the previous scheme's
// onSurfaceVariant usage.
//
// The old teal-prefixed names are gone; nothing outside this file used them.

private val NoirLight = lightColorScheme(
    primary = Noir.Ink,
    onPrimary = Noir.Paper,
    primaryContainer = Noir.PaperSunk,
    onPrimaryContainer = Noir.Ink,
    inversePrimary = Noir.Ivory,
    secondary = Noir.InkSoft,
    onSecondary = Noir.Paper,
    secondaryContainer = Noir.PaperSunk,
    onSecondaryContainer = Noir.Ink,
    tertiary = Noir.GoldOnPaper,
    onTertiary = Noir.PaperRaised,
    tertiaryContainer = Color(0xFFF1E6CF),
    onTertiaryContainer = Color(0xFF3D2E14),
    background = Noir.Paper,
    onBackground = Noir.Ink,
    surface = Noir.Paper,
    onSurface = Noir.Ink,
    surfaceVariant = Noir.PaperCard,
    onSurfaceVariant = Noir.InkMute,
    surfaceTint = Color.Transparent,
    inverseSurface = Noir.Ink,
    inverseOnSurface = Noir.Ivory,
    error = Color(0xFFB4442E),
    onError = Color.White,
    errorContainer = Color(0xFFF6DED6),
    onErrorContainer = Color(0xFF4A140A),
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
    tertiary = Noir.GoldOnOnyx,
    onTertiary = Noir.Onyx,
    tertiaryContainer = Color(0xFF3A2F1C),
    onTertiaryContainer = Noir.GoldLight,
    background = Noir.Onyx,
    onBackground = Noir.Ivory,
    surface = Noir.Onyx,
    onSurface = Noir.Ivory,
    surfaceVariant = Noir.Onyx1,
    onSurfaceVariant = Noir.IvoryMute,
    surfaceTint = Color.Transparent,
    inverseSurface = Noir.Ivory,
    inverseOnSurface = Noir.Onyx,
    error = Color(0xFFE8826A),
    onError = Noir.Onyx,
    errorContainer = Color(0xFF3B1D16),
    onErrorContainer = Color(0xFFF6CFC4),
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
