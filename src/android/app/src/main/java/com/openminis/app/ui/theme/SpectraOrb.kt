package com.openminis.app.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.openminis.app.ui.components.decorativePhase
import com.openminis.app.ui.components.decorativePingPong
import com.openminis.app.ui.components.rememberDecorativeTick

/**
 * [T-spectra-v3] The assistant orb — the single place the spectrum lives.
 *
 * A conic ring (sweep gradient) around a pearl core with a specular
 * highlight and two soft tints. States:
 *  • Idle  — still ring.
 *  • Think — violet/pink ring, rotating, breathing halo.
 *  • Live  — full spectrum ring, rotating, faint halo (tool running).
 *  • Done  — desaturated, still.
 *  • Error — rose ring.
 *  • Muted — greyscale (interrupted).
 *
 * Animated states read the shared 30 fps decorative tick inside draw only,
 * so an orb never recomposes per frame (see DecorativeAnimation.kt).
 */
enum class OrbState { Idle, Think, Live, Done, Error, Muted }

@Composable
fun SpectraOrb(
    size: Dp,
    modifier: Modifier = Modifier,
    state: OrbState = OrbState.Idle,
) {
    val dark = LocalChatPalette.current.isDark
    val animated = state == OrbState.Think || state == OrbState.Live
    if (animated) {
        val tick = rememberDecorativeTick()
        Box(
            modifier
                .size(size)
                .drawBehind {
                    val t = tick.value
                    val angle = 210f + 360f * decorativePhase(t, if (state == OrbState.Think) 3200 else 4200, 0)
                    val glow = decorativePingPong(decorativePhase(t, 2400, 0))
                    drawSpectraOrb(state, dark, angle, glow)
                },
        )
    } else {
        Box(
            modifier
                .size(size)
                .drawBehind { drawSpectraOrb(state, dark, 210f, 0f) },
        )
    }
}

private val Grey = Color(0xFF9A9CA3)

private fun ringColors(state: OrbState): List<Color> = when (state) {
    OrbState.Think -> Noir.SpectrumThink
    OrbState.Error -> Noir.SpectrumErr
    OrbState.Done -> Noir.Spectrum.map { lerp(it, Grey, 0.32f) }
    OrbState.Muted -> Noir.Spectrum.map { lerp(it, Grey, 0.85f) }
    else -> Noir.Spectrum
}

/**
 * Draws the orb centred in the current DrawScope. [angleDeg] rotates the
 * ring; [glow] 0..1 drives the halo for animated states. The halo is drawn
 * outside the bounds on purpose (callers do not clip).
 */
fun DrawScope.drawSpectraOrb(
    state: OrbState,
    dark: Boolean,
    angleDeg: Float = 210f,
    glow: Float = 0f,
) {
    val r = size.minDimension / 2f
    val c = Offset(size.width / 2f, size.height / 2f)
    val ring = ringColors(state)

    if (state == OrbState.Think || state == OrbState.Live) {
        val haloR = r * (1.35f + 0.25f * glow)
        val haloColor = if (state == OrbState.Think) Color(0xFFC5B2FF) else Color(0xFF9CBBFF)
        val a = (if (state == OrbState.Think) 0.30f else 0.20f) + 0.25f * glow
        drawCircle(
            brush = Brush.radialGradient(
                0f to haloColor.copy(alpha = a),
                0.55f to haloColor.copy(alpha = a * 0.35f),
                1f to Color.Transparent,
                center = c,
                radius = haloR,
            ),
            radius = haloR,
            center = c,
        )
    }

    rotate(angleDeg - 210f, pivot = c) {
        drawCircle(brush = Brush.sweepGradient(ring, center = c), radius = r, center = c)
    }

    // Pearl core.
    val inner = r * 0.76f
    val core = if (dark) {
        listOf(Color(0xFF1E2028), Color(0xFF0F1014), Color(0xFF08080A))
    } else {
        listOf(Color(0xFFFFFFFF), Color(0xFFF1F2F7), Color(0xFFE3E6EF))
    }
    drawCircle(
        brush = Brush.radialGradient(
            0f to core[0], 0.62f to core[1], 1f to core[2],
            center = Offset(c.x, c.y + inner * 0.04f), radius = inner,
        ),
        radius = inner,
        center = c,
    )
    if (state != OrbState.Muted) {
        // Violet tint, lower right.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFC5B2FF).copy(alpha = if (dark) 0.50f else 0.55f), Color.Transparent),
                center = Offset(c.x + inner * 0.40f, c.y + inner * 0.56f),
                radius = inner * 0.95f,
            ),
            radius = inner,
            center = c,
        )
        // Aqua tint, lower left.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF8FE3D2).copy(alpha = if (dark) 0.35f else 0.45f), Color.Transparent),
                center = Offset(c.x - inner * 0.52f, c.y + inner * 0.44f),
                radius = inner * 0.85f,
            ),
            radius = inner,
            center = c,
        )
    }
    // Specular highlight, upper left.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = if (dark) 0.50f else 0.95f), Color.Transparent),
            center = Offset(c.x - inner * 0.36f, c.y - inner * 0.48f),
            radius = inner * 0.62f,
        ),
        radius = inner,
        center = c,
    )
}

/** Convenience: default orb sizes used across the app. */
object OrbSize {
    val Inline = 18.dp
    val Header = 26.dp
    val Island = 30.dp
    val Hero = 84.dp
}
