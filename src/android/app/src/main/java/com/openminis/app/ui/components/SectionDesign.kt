package com.openminis.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.openminis.app.ui.theme.ExpressiveTokens

/**
 * Material 3 Expressive Section & Articulated Card Design Tokens.
 *
 * Replaces the legacy iOS SwiftUI grouped-form tokens (12dp white cards + 0.5dp
 * hairline inset dividers) with Google Material 3 Expressive articulated bento
 * groups: 26dp outer silhouette, 6dp inner corner articulation, and 3dp tonal
 * canvas seams between stacked items.
 */
object SectionDesign {
    val ScreenHorizontalPadding = 16.dp
    val CardShape = RoundedCornerShape(ExpressiveTokens.OuterGroupRadius)

    val FirstSectionTopGap = 14.dp
    val SectionTopGap = 24.dp
    val HeaderToCardGap = 10.dp
    val CardToFooterGap = 8.dp
    val InterRowGap = 8.dp

    val RowMinHeight = 52.dp
    val TextFieldMinHeight = 56.dp

    val RowHorizontalPadding = 18.dp
    val RowVerticalPadding = 12.dp
    val CardInnerVerticalPadding = 2.dp

    val DividerThickness = ExpressiveTokens.GroupItemGap
    val DividerStartInset = 0.dp

    @Composable
    @ReadOnlyComposable
    fun cardColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow

    @Composable
    @ReadOnlyComposable
    fun screenBackgroundColor(): Color = MaterialTheme.colorScheme.background

    @Composable
    @ReadOnlyComposable
    fun dividerColor(): Color = MaterialTheme.colorScheme.background

    @Composable
    @ReadOnlyComposable
    fun footerColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = SectionDesign.ScreenHorizontalPadding + 6.dp,
            end = SectionDesign.ScreenHorizontalPadding,
            bottom = SectionDesign.HeaderToCardGap,
        ),
    )
}

@Composable
fun SectionFooter(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = SectionDesign.footerColor(),
        modifier = modifier.padding(
            start = SectionDesign.ScreenHorizontalPadding + 6.dp,
            end = SectionDesign.ScreenHorizontalPadding + 6.dp,
            top = SectionDesign.CardToFooterGap,
        ),
    )
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = SectionDesign.cardColor(),
        shape = SectionDesign.CardShape,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SectionDesign.ScreenHorizontalPadding),
    ) {
        Column(
            modifier = Modifier.padding(vertical = SectionDesign.CardInnerVerticalPadding),
            content = content,
        )
    }
}

/**
 * Material 3 Expressive articulated group seam.
 *
 * Instead of an iOS 0.5dp inset line, renders a [gapHeight] (3dp) seam in
 * [seamColor] (matching the page canvas) and sculpts 6dp inner rounded corners
 * onto both the item above and the item below. Every multi-row card across the
 * app automatically becomes a segmented M3 Expressive articulated stack!
 */
@Composable
fun ExpressiveArticulatedSeam(
    modifier: Modifier = Modifier,
    seamColor: Color = MaterialTheme.colorScheme.background,
    gapHeight: Dp = ExpressiveTokens.GroupItemGap,
    innerRadius: Dp = ExpressiveTokens.InnerGroupRadius,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(gapHeight)
            .zIndex(1f),
    ) {
        val w = size.width
        val gap = size.height
        val r = innerRadius.toPx().coerceAtMost(w / 4f)

        // 1. Horizontal canvas gap between stacked segments
        drawRect(
            color = seamColor,
            topLeft = Offset.Zero,
            size = size,
        )

        if (r > 0.5f) {
            fun cornerFillet(rect: Rect, circleCenter: Offset): Path {
                val box = Path().apply { addRect(rect) }
                val disc = Path().apply {
                    addOval(Rect(center = circleCenter, radius = r))
                }
                return Path().apply { op(box, disc, PathOperation.Difference) }
            }

            // Upper segment bottom-left & bottom-right 6dp corners
            drawPath(
                path = cornerFillet(Rect(0f, -r, r, 0f), Offset(r, -r)),
                color = seamColor,
            )
            drawPath(
                path = cornerFillet(Rect(w - r, -r, w, 0f), Offset(w - r, -r)),
                color = seamColor,
            )
            // Lower segment top-left & top-right 6dp corners
            drawPath(
                path = cornerFillet(Rect(0f, gap, r, gap + r), Offset(r, gap + r)),
                color = seamColor,
            )
            drawPath(
                path = cornerFillet(Rect(w - r, gap, w, gap + r), Offset(w - r, gap + r)),
                color = seamColor,
            )
        }
    }
}

/** Articulated seam between rows inside a [SectionCard]. */
@Composable
fun SectionDivider() {
    ExpressiveArticulatedSeam()
}

@Composable
fun RowLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = 6.dp),
    )
}
