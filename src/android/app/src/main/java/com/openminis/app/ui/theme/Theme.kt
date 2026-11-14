package com.openminis.app.ui.theme

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.ui.components.decorativePhase
import com.openminis.app.ui.components.decorativePingPong
import com.openminis.app.ui.components.rememberDecorativeTick

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
    onTertiaryContainer = Color(0xFF3A0022),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF171B24),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF171B24),
    surfaceVariant = Color(0xFFE0E5F5),
    onSurfaceVariant = Color(0xFF434857),
    outline = Color(0xFF737888),
    outlineVariant = Color(0xFFC3C8D9),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2C303A),
    inverseOnSurface = Color(0xFFEEF0FA),
    surfaceBright = Color(0xFFF8F9FF),
    surfaceDim = Color(0xFFD7DAE5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3FD),
    surfaceContainer = Color(0xFFEBEEF8),
    surfaceContainerHigh = Color(0xFFE5E8F3),
    surfaceContainerHighest = Color(0xFFDFE2ED),
)

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB5C4FF),
    onPrimary = Color(0xFF00297B),
    primaryContainer = Color(0xFF123FBF),
    onPrimaryContainer = Color(0xFFDCE3FF),
    inversePrimary = Color(0xFF2956D8),
    secondary = Color(0xFFB3C7F2),
    onSecondary = Color(0xFF1A2F52),
    secondaryContainer = Color(0xFF304569),
    onSecondaryContainer = Color(0xFFD9E2FF),
    tertiary = Color(0xFFFFB0D3),
    onTertiary = Color(0xFF541239),
    tertiaryContainer = Color(0xFF6F2950),
    onTertiaryContainer = Color(0xFFFFD8E8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F131B),
    onBackground = Color(0xFFE0E2EE),
    surface = Color(0xFF0F131B),
    onSurface = Color(0xFFE0E2EE),
    surfaceVariant = Color(0xFF424756),
    onSurfaceVariant = Color(0xFFC2C6D8),
    outline = Color(0xFF8C91A2),
    outlineVariant = Color(0xFF424756),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE0E2EE),
    inverseOnSurface = Color(0xFF2C303A),
    surfaceBright = Color(0xFF353943),
    surfaceDim = Color(0xFF0F131B),
    surfaceContainerLowest = Color(0xFF0A0E16),
    surfaceContainerLow = Color(0xFF171B24),
    surfaceContainer = Color(0xFF1B2029),
    surfaceContainerHigh = Color(0xFF252A34),
    surfaceContainerHighest = Color(0xFF303540),
)

// ============================================================================
// Material 3 Expressive Shape Architecture & Articulated List Helpers
// ============================================================================

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

object ExpressiveTokens {
    /** Outer corner radius of an articulated M3 Expressive card group. */
    val OuterGroupRadius: Dp = 26.dp
    /** Inner ("kissing") corner radius between adjacent items in a group. */
    val InnerGroupRadius: Dp = 6.dp
    /** Gap between articulated sibling cards inside a group. */
    val GroupItemGap: Dp = 2.dp
    /** Top corner radius of M3 Expressive modal sheets. */
    val SheetTopRadius: Dp = 32.dp
    /** Standard horizontal gutter for screen-level bento layouts. */
    val ScreenHorizontalPadding: Dp = 16.dp
}

/**
 * Computes the Material 3 Expressive articulated shape for item [index] inside
 * a group of [count] items:
 * - Single item (`count == 1`): fully rounded `OuterGroupRadius`
 * - First item: large top corners + crisp `InnerGroupRadius` bottom corners
 * - Middle items: crisp `InnerGroupRadius` on all 4 corners
 * - Last item: crisp `InnerGroupRadius` top corners + large bottom corners
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
 * Decorative Material 3 Expressive polygon shapes from `MaterialShapes` for
 * hero badges, category avatars, onboarding emblems, and active indicators.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun expressiveBadgeShape(seed: Int = 0): Shape {
    val polygon = when (seed.mod(5)) {
        0 -> MaterialShapes.Cookie9Sided
        1 -> MaterialShapes.Sunny
        2 -> MaterialShapes.SoftBurst
        3 -> MaterialShapes.Clover4Leaf
        else -> MaterialShapes.Cookie12Sided
    }
    return polygon.toShape()
}

/**
 * Material 3 Expressive loading indicator powered by [rememberDecorativeTick].
 *
 * Uses GPU-layer transforms (`graphicsLayer`) on the shared ~30 fps
 * [com.openminis.app.ui.components.DecorativeClock] rather than alpha
 * `LoadingIndicator`'s per-frame `Morph` path allocations and `SuspendAnimation`
 * spring loops — guaranteeing zero per-frame recomposition, zero re-layout, and
 * immunity to main-thread hangs when system `ANIMATOR_DURATION_SCALE == 0f`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val tick = rememberDecorativeTick()
    val badgeShape = MaterialShapes.Sunny.toShape()
    val containerTint = remember(color) { color.copy(alpha = 0.20f) }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.94f)
                .graphicsLayer {
                    val phase = decorativePhase(tick.value, 2400)
                    val pulse = 0.88f + 0.12f * decorativePingPong(phase * 2f)
                    rotationZ = phase * 360f
                    scaleX = pulse
                    scaleY = pulse
                }
                .background(containerTint, badgeShape),
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.66f)
                .graphicsLayer {
                    rotationZ = decorativePhase(tick.value, 900) * 360f
                },
        ) {
            val strokePx = (size.minDimension * 0.15f).coerceIn(1.75.dp.toPx(), 3.5.dp.toPx())
            val inset = strokePx / 2f
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 255f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(
                    (size.width - strokePx).coerceAtLeast(1f),
                    (size.height - strokePx).coerceAtLeast(1f),
                ),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
    }
}

// ============================================================================
// Material 3 Expressive Editorial Typography Scale
// High-contrast display/headline weights + crisp readable body/label rhythm.
// ============================================================================

private val ExpressiveBaseTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 54.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontSize = 42.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.15).sp,
    ),
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.2).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.15).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.1).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.05).sp,
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
        lineHeight = 20.sp,
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

    MaterialTheme(
        colorScheme = colorScheme,
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
