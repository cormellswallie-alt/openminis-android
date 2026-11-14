package com.openminis.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Material 3 Expressive floating context menu with spring scale entrance,
 * 24dp rounded geometry, and elevated `surfaceContainerHigh` tonal container.
 */
@Composable
fun MinisMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation: Dp = 4.dp,
    shadowElevation: Dp = 20.dp,
    border: BorderStroke? = BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    ),
    minWidth: Dp = 192.dp,
    alignEnd: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val density = LocalDensity.current
    val offsetXPx = with(density) { offset.x.roundToPx() }
    val offsetYPx = with(density) { offset.y.roundToPx() }
    Popup(
        onDismissRequest = onDismissRequest,
        properties = properties,
        popupPositionProvider = remember(offsetXPx, offsetYPx, alignEnd) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    val x = if (alignEnd) {
                        (anchorBounds.right - popupContentSize.width + offsetXPx)
                    } else {
                        (anchorBounds.left + offsetXPx)
                    }.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                    val belowY = anchorBounds.bottom + offsetYPx
                    val aboveY = anchorBounds.top - popupContentSize.height - offsetYPx
                    val fitsBelow = belowY + popupContentSize.height <= windowSize.height
                    val y = if (fitsBelow || aboveY < 0) belowY else aboveY
                    return IntOffset(
                        x,
                        y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
                    )
                }
            }
        },
    ) {
        val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
        val origin = if (alignEnd) TransformOrigin(1f, 0f) else TransformOrigin(0f, 0f)
        AnimatedVisibility(
            visibleState = visibleState,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
                initialScale = 0.86f,
                transformOrigin = origin,
            ) + fadeIn(animationSpec = tween(150)),
            exit = scaleOut(animationSpec = tween(110)) + fadeOut(animationSpec = tween(110)),
        ) {
            Surface(
                modifier = modifier
                    .width(IntrinsicSize.Max)
                    .widthIn(min = minWidth, max = 296.dp)
                    .shadow(
                        elevation = shadowElevation,
                        shape = shape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.28f),
                        spotColor = Color.Black.copy(alpha = 0.28f),
                    ),
                shape = shape,
                color = containerColor,
                tonalElevation = tonalElevation,
                shadowElevation = 0.dp,
                border = border,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(vertical = 6.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun MinisMenuDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
}

object MinisMenuDefaults {
    val ItemPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 2.dp)

    @Composable
    fun itemColors(
        textColor: Color = MaterialTheme.colorScheme.onSurface,
        leadingIconColor: Color = MaterialTheme.colorScheme.primary,
        trailingIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    ): MenuItemColors = MenuDefaults.itemColors(
        textColor = textColor,
        leadingIconColor = leadingIconColor,
        trailingIconColor = trailingIconColor,
    )

    @Composable
    fun destructiveItemColors(): MenuItemColors = MenuDefaults.itemColors(
        textColor = MaterialTheme.colorScheme.error,
        leadingIconColor = MaterialTheme.colorScheme.error,
        trailingIconColor = MaterialTheme.colorScheme.error,
    )
}
