package com.openminis.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star

/**
 * Pure `androidx.graphics:graphics-shapes:1.0.1` implementation of the Material 3
 * Expressive polygon catalog (`Sunny`, `Cookie9Sided`, `Cookie12Sided`, `SoftBurst`,
 * `Clover4Leaf`, `Pentagon`, `Arch`, `Gem`, `Heart`, `Diamond`, `Fan`, `Flower`,
 * `PuffyDiamond`, `Cookie6Sided`, `Cookie7Sided`).
 *
 * Keeping this directly on stable `graphics-shapes:1.0.1` avoids pulling alpha
 * `androidx.compose.material3:material3:1.5.0-alpha*` into the runtime dependency
 * graph (which otherwise forces `savedstate:1.3.0` + `lifecycle:2.9.0` against
 * `navigation-compose:2.8.5`), and avoids mutating a shared `Path` across layout
 * passes.
 */
object MaterialShapes {
    private fun safeNormalized(builder: () -> RoundedPolygon): RoundedPolygon =
        runCatching { builder().normalized() }
            .getOrElse { RoundedPolygon.circle(numVertices = 8).normalized() }

    val Sunny: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = 0.80f,
                rounding = CornerRounding(radius = 0.16f, smoothing = 0.40f),
                innerRounding = CornerRounding(radius = 0.16f, smoothing = 0.40f),
            )
        }
    }

    val Cookie6Sided: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon.star(
                numVerticesPerRadius = 6,
                innerRadius = 0.78f,
                rounding = CornerRounding(radius = 0.24f, smoothing = 0.50f),
                innerRounding = CornerRounding(radius = 0.24f, smoothing = 0.50f),
            )
        }
    }

    val Cookie7Sided: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon.star(
                numVerticesPerRadius = 7,
                innerRadius = 0.78f,
                rounding = CornerRounding(radius = 0.22f, smoothing = 0.50f),
                innerRounding = CornerRounding(radius = 0.22f, smoothing = 0.50f),
            )
        }
    }

    val Cookie9Sided: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon.star(
                numVerticesPerRadius = 9,
                innerRadius = 0.80f,
                rounding = CornerRounding(radius = 0.20f, smoothing = 0.50f),
                innerRounding = CornerRounding(radius = 0.20f, smoothing = 0.50f),
            )
        }
    }

    val Cookie12Sided: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon.star(
                numVerticesPerRadius = 12,
                innerRadius = 0.82f,
                rounding = CornerRounding(radius = 0.16f, smoothing = 0.50f),
                innerRounding = CornerRounding(radius = 0.16f, smoothing = 0.50f),
            )
        }
    }

    val SoftBurst: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon.star(
                numVerticesPerRadius = 10,
                innerRadius = 0.86f,
                rounding = CornerRounding(radius = 0.14f, smoothing = 0.45f),
                innerRounding = CornerRounding(radius = 0.14f, smoothing = 0.45f),
            )
        }
    }

    val Clover4Leaf: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                innerRadius = 0.62f,
                rounding = CornerRounding(radius = 0.36f, smoothing = 0.60f),
                innerRounding = CornerRounding(radius = 0.20f, smoothing = 0.40f),
            )
        }
    }

    val Flower: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = 0.72f,
                rounding = CornerRounding(radius = 0.28f, smoothing = 0.55f),
                innerRounding = CornerRounding(radius = 0.18f, smoothing = 0.45f),
            )
        }
    }

    val Pentagon: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon(
                numVertices = 5,
                rounding = CornerRounding(radius = 0.28f, smoothing = 0.45f),
            )
        }
    }

    val Gem: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon(
                numVertices = 6,
                rounding = CornerRounding(radius = 0.24f, smoothing = 0.40f),
            )
        }
    }

    val Diamond: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon(
                numVertices = 4,
                rounding = CornerRounding(radius = 0.26f, smoothing = 0.35f),
            )
        }
    }

    val PuffyDiamond: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                innerRadius = 0.82f,
                rounding = CornerRounding(radius = 0.32f, smoothing = 0.65f),
                innerRounding = CornerRounding(radius = 0.26f, smoothing = 0.55f),
            )
        }
    }

    val Arch: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon(
                numVertices = 4,
                perVertexRounding = listOf(
                    CornerRounding(radius = 0.50f, smoothing = 0.50f),
                    CornerRounding(radius = 0.50f, smoothing = 0.50f),
                    CornerRounding(radius = 0.20f, smoothing = 0.25f),
                    CornerRounding(radius = 0.20f, smoothing = 0.25f),
                ),
            )
        }
    }

    val Fan: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon(
                numVertices = 4,
                perVertexRounding = listOf(
                    CornerRounding(radius = 0.62f, smoothing = 0.55f),
                    CornerRounding(radius = 0.22f, smoothing = 0.30f),
                    CornerRounding(radius = 0.22f, smoothing = 0.30f),
                    CornerRounding(radius = 0.22f, smoothing = 0.30f),
                ),
            )
        }
    }

    val Heart: RoundedPolygon by lazy {
        safeNormalized {
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                innerRadius = 0.74f,
                rounding = CornerRounding(radius = 0.34f, smoothing = 0.60f),
                innerRounding = CornerRounding(radius = 0.22f, smoothing = 0.45f),
            )
        }
    }
}

internal fun RoundedPolygon.buildScaledComposePath(width: Float, height: Float): Path {
    val path = Path()
    val curves = cubics
    if (curves.isEmpty() || width <= 0f || height <= 0f) return path
    path.moveTo(curves[0].anchor0X * width, curves[0].anchor0Y * height)
    for (i in curves.indices) {
        val c = curves[i]
        path.cubicTo(
            c.control0X * width,
            c.control0Y * height,
            c.control1X * width,
            c.control1Y * height,
            c.anchor1X * width,
            c.anchor1Y * height,
        )
    }
    path.close()
    return path
}

/**
 * Converts a normalized [RoundedPolygon] into a Compose [Shape] that allocates a
 * fresh immutable [Path] only when the target layout [Size] changes.
 */
@Composable
fun RoundedPolygon.toShape(): Shape = remember(this) {
    val polygon = this
    object : Shape {
        private var cachedWidth = -1f
        private var cachedHeight = -1f
        private var cachedOutline: Outline? = null

        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline {
            val w = size.width
            val h = size.height
            if (!w.isFinite() || !h.isFinite() || w <= 0f || h <= 0f) {
                return Outline.Rectangle(Rect.Zero)
            }
            val existing = cachedOutline
            if (existing != null && cachedWidth == w && cachedHeight == h) {
                return existing
            }
            return runCatching {
                val path = polygon.buildScaledComposePath(w, h)
                if (path.isEmpty) {
                    CircleShape.createOutline(size, layoutDirection, density)
                } else {
                    Outline.Generic(path).also {
                        cachedWidth = w
                        cachedHeight = h
                        cachedOutline = it
                    }
                }
            }.getOrElse {
                CircleShape.createOutline(size, layoutDirection, density)
            }
        }
    }
}
