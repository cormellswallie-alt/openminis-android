package com.openminis.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Google Material 3 Expressive Color System
// Signature palette: Electric Cobalt-Iris primary + Cool Slate secondary +
// Plum-Orchid tertiary + full 7-tier tonal surface container ramp.
// ============================================================================

private val ExpressiveLightColorScheme = lightColorScheme(
    primary = Color(0xFF2956D8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE3FF),
    onPrimaryContainer = Color(0xFF00164E),
    inversePrimary = Color(0xFFB5C4FF),
    secondary = Color(0xFF475D82),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E2FF),
    onSecondaryContainer = Color(0xFF0E1B37),
    tertiary = Color(0xFF8C4168),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E8),
    onTertiaryContainer = Color(0xFF390022),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F7FC),
    onBackground = Color(0xFF171B23),
    surface = Color(0xFFF6F7FC),
    onSurface = Color(0xFF171B23),
    surfaceVariant = Color(0xFFE0E3F0),
    onSurfaceVariant = Color(0xFF434753),
    surfaceDim = Color(0xFFD7DAE4),
    surfaceBright = Color(0xFFFCF8FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F2FA),
    surfaceContainer = Color(0xFFEAEDF7),
    surfaceContainerHigh = Color(0xFFE3E7F3),
    surfaceContainerHighest = Color(0xFFDCE1EF),
    outline = Color(0xFF737785),
    outlineVariant = Color(0xFFC3C6D6),
    inverseSurface = Color(0xFF2C3039),
    inverseOnSurface = Color(0xFFEEF0FA),
    scrim = Color(0xFF000000),
)

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB5C4FF),
    onPrimary = Color(0xFF00287D),
    primaryContainer = Color(0xFF1E3FA8),
    onPrimaryContainer = Color(0xFFDCE3FF),
    inversePrimary = Color(0xFF2956D8),
    secondary = Color(0xFFB8C6EA),
    onSecondary = Color(0xFF22304F),
    secondaryContainer = Color(0xFF334263),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFFFFB0D0),
    onTertiary = Color(0xFF541238),
    tertiaryContainer = Color(0xFF6F294F),
    onTertiaryContainer = Color(0xFFFFD8E8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F131B),
    onBackground = Color(0xFFE2E5F0),
    surface = Color(0xFF0F131B),
    onSurface = Color(0xFFE2E5F0),
    surfaceVariant = Color(0xFF434754),
    onSurfaceVariant = Color(0xFFC3C6D6),
    surfaceDim = Color(0xFF0F131B),
    surfaceBright = Color(0xFF353944),
    surfaceContainerLowest = Color(0xFF0A0D14),
    surfaceContainerLow = Color(0xFF171B24),
    surfaceContainer = Color(0xFF1D222D),
    surfaceContainerHigh = Color(0xFF272C3A),
    surfaceContainerHighest = Color(0xFF323848),
    outline = Color(0xFF8D91A0),
    outlineVariant = Color(0xFF434754),
    inverseSurface = Color(0xFFE2E5F0),
    inverseOnSurface = Color(0xFF2C3039),
    scrim = Color(0xFF000000),
)

/** Expressive primary FAB container color (replaces legacy iOS warm beige). */
@Composable
fun minisFabColor(): Color = MaterialTheme.colorScheme.primaryContainer

// ============================================================================
// Material 3 Expressive Shape Architecture & Articulated Group Helpers
// ============================================================================

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object ExpressiveTokens {
    val OuterGroupRadius = 26.dp
    val InnerGroupRadius = 6.dp
    val GroupItemGap = 3.dp
    val BentoCornerRadius = 26.dp
    val SheetTopRadius = 32.dp
    val PillCornerRadius = 999.dp
}

/**
 * Computes the signature Material 3 Expressive articulated card shape for item [index]
 * inside a vertical segmented stack of [count] items:
 * - Single item: 26dp all around
 * - First item:  26dp top, 6dp bottom
 * - Middle item: 6dp all around
 * - Last item:   6dp top, 26dp bottom
 */
fun articulatedItemShape(
    index: Int,
    count: Int,
    outerRadius: Dp = ExpressiveTokens.OuterGroupRadius,
    innerRadius: Dp = ExpressiveTokens.InnerGroupRadius,
): RoundedCornerShape {
    if (count <= 1) return RoundedCornerShape(outerRadius)
    return when (index) {
        0 -> RoundedCornerShape(
            topStart = outerRadius,
            topEnd = outerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius,
        )
        count - 1 -> RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = outerRadius,
            bottomEnd = outerRadius,
        )
        else -> RoundedCornerShape(innerRadius)
    }
}

/**
 * Returns a distinctive Material 3 Expressive polygon [Shape] for category badges,
 * avatars, and hero accents using `MaterialShapes`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun expressivePolygonShape(seed: Int): Shape {
    val polygon = when (seed.mod(8)) {
        0 -> MaterialShapes.Sunny
        1 -> MaterialShapes.Cookie9Sided
        2 -> MaterialShapes.Clover4Leaf
        3 -> MaterialShapes.Pentagon
        4 -> MaterialShapes.SoftBurst
        5 -> MaterialShapes.Cookie12Sided
        6 -> MaterialShapes.Arch
        else -> MaterialShapes.Gem
    }
    return polygon.toShape()
}

// ============================================================================
// Material 3 Expressive Editorial Typography Architecture
// High-contrast display/headline hierarchy with tight negative tracking,
// confident semi-bold titles/labels, and generous readable body leading.
// ============================================================================

private val ExpressiveBaseTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 54.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1.2).sp,
    ),
    displayMedium = TextStyle(
        fontSize = 42.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.8).sp,
    ),
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 25.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 21.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.15).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.05.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.25.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.35.sp,
    ),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MinisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1f,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ExpressiveDarkColorScheme
        else -> ExpressiveLightColorScheme
    }
    val typography = scaledExpressiveTypography(fontScale)
    val chatPalette = expressiveChatPalette(colorScheme, darkTheme)

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = typography,
    ) {
        CompositionLocalProvider(LocalChatPalette provides chatPalette, content = content)
    }
}

private fun TextStyle.scale(factor: Float): TextStyle =
    if (factor == 1f) this else copy(
        fontSize = fontSize * factor,
        lineHeight = lineHeight * factor,
    )

private fun scaledExpressiveTypography(factor: Float): Typography {
    val base = ExpressiveBaseTypography
    if (factor == 1f) return base
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
