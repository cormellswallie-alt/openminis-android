package com.openminis.app.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import kotlinx.coroutines.launch

/**
 * [T-noir-redesign] Launch sequence: the pearl surfaces, the serif wordmark
 * resolves above a iris hairline, then the curtain lifts onto the app,
 * which has been composing underneath the whole time (so the overlay never
 * delays first data load — it only covers it for ~1.4 s).
 *
 * Timeline (fraction of [TOTAL_MS]):
 *   0.00–0.40  pearl rises: scale 0.55→1, alpha 0→1 (expo-out)
 *   0.08–0.55  iris halo blooms behind it
 *   0.22–0.56  wordmark fades up 10dp
 *   0.36–0.66  gold rule draws out from the centre
 *   0.66–0.74  hold
 *   0.74–1.00  curtain: overlay fades, content lifts 18dp and scales to 1.06
 *
 * Every frame-varying value is read inside graphicsLayer / draw lambdas, so
 * the overlay never recomposes while animating. A tap skips straight to the
 * curtain. Shown only on a true cold start of the launcher entry point (see
 * [shouldShow]); never on recreation, deep links, shares or shortcuts.
 */
object NoirLaunch {
    /** Appearance pref (in appearance_prefs). Default on. */
    const val KEY_LAUNCH_ANIMATION = "noir_launch_animation"

    internal const val TOTAL_MS = 1700
    internal const val EXIT = 0.74f

    fun isEnabled(context: Context): Boolean =
        com.openminis.app.ui.settings.getAppearancePrefs(context)
            .getBoolean(KEY_LAUNCH_ANIMATION, true)

    /** False when the user turned system animations off (Developer options / accessibility). */
    fun systemAnimationsEnabled(context: Context): Boolean = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    } catch (_: Throwable) {
        true
    }

    fun shouldShow(context: Context, coldStart: Boolean, launcherEntry: Boolean): Boolean =
        coldStart && launcherEntry && isEnabled(context) && systemAnimationsEnabled(context)
}

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseInOutQuart = CubicBezierEasing(0.76f, 0f, 0.24f, 1f)

private fun seg(t: Float, start: Float, end: Float): Float =
    ((t - start) / (end - start)).coerceIn(0f, 1f)

/*
 * [T-spectra-v3] Launch sequence "Ignition".
 *
 * The OS splash (API 31+) paints only a hairline circle at window centre.
 * This overlay starts from that exact frame and lights it:
 *
 *   0.00–0.40  a spectrum arc draws the ring clockwise from 12 o'clock;
 *              a small bright spark rides the leading tip
 *   0.30–0.52  the pearl core swells in and the ring closes into the orb
 *   0.44–0.74  one soft ripple leaves the orb and fades; halo breathes once
 *   0.42–0.68  "Minis" rises letter by letter below the orb
 *   0.74–1.00  exit: orb lifts and shrinks, wordmark fades, ground dissolves
 *
 * Only graphicsLayer / draw lambdas read the clock, so nothing recomposes
 * per frame. Tap skips to the exit. Same gating as before (cold launcher
 * start + setting on + system animations on).
 */
@Composable
fun NoirLaunchOverlay(
    isDark: Boolean,
    onFinished: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val finish by rememberUpdatedState(onFinished)
    var done by remember { mutableStateOf(false) }
    val complete = {
        if (!done) {
            done = true
            finish()
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(NoirLaunch.TOTAL_MS, easing = LinearEasing))
        complete()
    }

    val ground = if (isDark) Noir.Onyx else Noir.PaperCard
    val ink = if (isDark) Noir.Ivory else Noir.Ink
    val hairline = if (isDark) Color(0x33FFFFFF) else Color(0x1F0A0A0B)
    val word = stringResource(R.string.app_name)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - EaseInOutQuart.transform(seg(progress.value, 0.80f, 1f))
            }
            .drawBehind { drawRect(ground) }
            .pointerInput(Unit) {
                detectTapGestures {
                    if (progress.value < NoirLaunch.EXIT) {
                        scope.launch {
                            progress.snapTo(NoirLaunch.EXIT)
                            progress.animateTo(1f, tween(360, easing = LinearEasing))
                            complete()
                        }
                    }
                }
            },
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            // ---- Orb (72dp at exact window centre, matches the splash ring) ----
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .graphicsLayer {
                        val e = EaseInOutQuart.transform(seg(progress.value, NoirLaunch.EXIT, 1f))
                        val s = 1f - 0.22f * e
                        scaleX = s
                        scaleY = s
                        translationY = -56.dp.toPx() * e
                    }
                    .drawBehind {
                        val t = progress.value
                        val r = size.minDimension / 2f
                        val c = center
                        val ringW = r * 0.24f
                        val arc = EaseOutExpo.transform(seg(t, 0.04f, 0.40f))
                        val coreIn = EaseOutExpo.transform(seg(t, 0.30f, 0.52f))
                        val spin = 360f * t * 0.55f

                        // Ripple + halo (outside bounds on purpose).
                        val rp = seg(t, 0.44f, 0.74f)
                        if (rp > 0f && rp < 1f) {
                            val rr = r * (1f + 1.9f * EaseOutExpo.transform(rp))
                            rotate(spin, pivot = c) {
                                drawCircle(
                                    brush = Brush.sweepGradient(Noir.Spectrum, center = c),
                                    radius = rr,
                                    center = c,
                                    alpha = 0.55f * (1f - rp),
                                    style = Stroke(width = 1.5.dp.toPx()),
                                )
                            }
                        }
                        val glow = seg(t, 0.36f, 0.62f).let { if (it < 0.5f) it * 2f else (1f - it) * 2f }
                        if (glow > 0f) {
                            val hr = r * 2.1f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0f to Color(0xFF9CBBFF).copy(alpha = 0.30f * glow),
                                    0.5f to Color(0xFFC5B2FF).copy(alpha = 0.12f * glow),
                                    1f to Color.Transparent,
                                    center = c,
                                    radius = hr,
                                ),
                                radius = hr,
                                center = c,
                            )
                        }

                        // Hairline circle — identical to the OS splash frame.
                        drawCircle(
                            color = hairline,
                            radius = r - ringW / 2f,
                            center = c,
                            alpha = 1f - coreIn,
                            style = Stroke(width = 1.dp.toPx()),
                        )

                        if (coreIn < 1f) {
                            // Drawing phase: spectrum arc along the ring track.
                            if (arc > 0f) {
                                rotate(spin - 90f, pivot = c) {
                                    drawArc(
                                        brush = Brush.sweepGradient(Noir.Spectrum, center = c),
                                        startAngle = 0f,
                                        sweepAngle = 360f * arc,
                                        useCenter = false,
                                        topLeft = Offset(c.x - r + ringW / 2f, c.y - r + ringW / 2f),
                                        size = androidx.compose.ui.geometry.Size(2f * r - ringW, 2f * r - ringW),
                                        style = Stroke(width = ringW, cap = StrokeCap.Round),
                                    )
                                }
                                // Spark on the leading tip.
                                if (arc < 0.999f) {
                                    val a = Math.toRadians((spin - 90f + 360f * arc).toDouble())
                                    val tr = r - ringW / 2f
                                    val tip = Offset(c.x + tr * kotlin.math.cos(a).toFloat(), c.y + tr * kotlin.math.sin(a).toFloat())
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(Color.White.copy(alpha = 0.95f), Color(0x00C5B2FF)),
                                            center = tip,
                                            radius = ringW * 1.6f,
                                        ),
                                        radius = ringW * 1.6f,
                                        center = tip,
                                    )
                                }
                            }
                        }
                        if (coreIn > 0f) {
                            // Closing phase: the full orb fades/swells in over the arc.
                            val s = 0.86f + 0.14f * coreIn
                            scale(s, pivot = c) {
                                drawContext.canvas.saveLayer(
                                    androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height).inflate(r),
                                    androidx.compose.ui.graphics.Paint().apply { alpha = coreIn },
                                )
                                drawSpectraOrb(OrbState.Idle, isDark, 210f + spin, 0f)
                                drawContext.canvas.restore()
                            }
                        }
                    },
            )

            // ---- Wordmark: letters rise one by one ----
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight / 2 + 36.dp + 26.dp)
                    .graphicsLayer {
                        val e = EaseInOutQuart.transform(seg(progress.value, NoirLaunch.EXIT, 0.92f))
                        alpha = 1f - e
                        translationY = -40.dp.toPx() * e
                    },
            ) {
                word.forEachIndexed { i, ch ->
                    val start = 0.42f + i * 0.035f
                    Text(
                        text = ch.toString(),
                        style = NoirDisplay.copy(fontSize = 30.sp, lineHeight = 36.sp),
                        color = ink,
                        modifier = Modifier.graphicsLayer {
                            val w = EaseOutExpo.transform(seg(progress.value, start, start + 0.16f))
                            alpha = w
                            translationY = 12.dp.toPx() * (1f - w)
                        },
                    )
                }
            }
        }
    }
}

/**
 * The Minis pearl: a nacre body with a violet lobe (lower right), a
 * cool silver lobe (lower left), soft terminator shading and a specular
 * highlight — the same recipe as the design boards' CSS pearl.
 * Fills the DrawScope's min dimension; casts its own soft shadow below.
 */
fun DrawScope.drawNoirPearl(isDark: Boolean) {
    val r = size.minDimension / 2f
    val c = center
    fun at(fx: Float, fy: Float) = Offset(size.width * fx, size.height * fy)

    // Contact shadow
    val sc = Offset(c.x, c.y + r * 0.62f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = if (isDark) 0.55f else 0.20f), Color.Transparent),
            center = sc,
            radius = r * 1.15f,
        ),
        radius = r * 1.15f,
        center = sc,
    )
    // Nacre body
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFF1F2FA),
            0.68f to Color(0xFFB9BDD6),
            1f to Color(0xFF7D82A6),
            center = c,
            radius = r,
        ),
        radius = r,
        center = c,
    )
    // Violet lobe
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xE69B6CF6), Color(0x009B6CF6)),
            center = at(0.72f, 0.76f),
            radius = r * 1.12f,
        ),
        radius = r,
        center = c,
    )
    // Cool silver lobe
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xCC4F9BF7), Color(0x004F9BF7)),
            center = at(0.22f, 0.82f),
            radius = r * 1.04f,
        ),
        radius = r,
        center = c,
    )
    // Terminator shading toward the lower-right rim
    drawCircle(
        brush = Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Color(0x59201C3C),
            center = at(0.40f, 0.36f),
            radius = r * 1.25f,
        ),
        radius = r,
        center = c,
    )
    // Specular highlight (kept inside the body)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
            center = at(0.34f, 0.28f),
            radius = r * 0.48f,
        ),
        radius = r,
        center = c,
    )
}
