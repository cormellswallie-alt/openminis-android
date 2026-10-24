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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import kotlinx.coroutines.launch

/**
 * [T-noir-redesign] Launch sequence: the pearl surfaces, the serif wordmark
 * resolves above a champagne hairline, then the curtain lifts onto the app,
 * which has been composing underneath the whole time (so the overlay never
 * delays first data load — it only covers it for ~1.4 s).
 *
 * Timeline (fraction of [TOTAL_MS]):
 *   0.00–0.40  pearl rises: scale 0.55→1, alpha 0→1 (expo-out)
 *   0.08–0.55  champagne halo blooms behind it
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

    internal const val TOTAL_MS = 1450
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

    val ground = if (isDark) Noir.Onyx else Noir.Paper
    val ink = if (isDark) Noir.Ivory else Noir.Ink
    val haloAlpha = if (isDark) 0.30f else 0.20f
    // API 31+: the system splash already shows the pearl (see values-v31/themes.xml).
    val pearlPreShown = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - EaseInOutQuart.transform(seg(progress.value, NoirLaunch.EXIT, 1f))
            }
            .drawBehind { drawRect(ground) }
            .pointerInput(Unit) {
                detectTapGestures {
                    if (progress.value < NoirLaunch.EXIT) {
                        scope.launch {
                            progress.snapTo(NoirLaunch.EXIT)
                            progress.animateTo(1f, tween(340, easing = LinearEasing))
                            complete()
                        }
                    }
                }
            },
    ) {
        // Exit transform for the whole group (pivot = window centre).
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val e = EaseInOutQuart.transform(seg(progress.value, NoirLaunch.EXIT, 1f))
                    val s = 1f + 0.06f * e
                    scaleX = s
                    scaleY = s
                    translationY = -18.dp.toPx() * e
                },
        ) {
            // The pearl sits at the exact window centre — the same spot and
            // size (76dp) as the API 31+ system splash icon
            // (drawable/noir_splash_pearl.xml), so the OS splash hands off
            // without a jump. There it is already on screen, so it skips the
            // rise and only the halo/wordmark/rule animate in.
            Box(
                modifier = Modifier.align(Alignment.Center).size(84.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Champagne halo — drawn past the 84dp bounds (no clip), radius ~120dp.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val h = EaseOutExpo.transform(seg(progress.value, 0.08f, 0.55f))
                    if (h > 0f) {
                        val r = 120.dp.toPx() * (0.6f + 0.4f * h)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Noir.Gold.copy(alpha = haloAlpha * h), Color.Transparent),
                                center = center,
                                radius = r,
                            ),
                            radius = r,
                            center = center,
                        )
                    }
                }
                Canvas(
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer {
                            val p = if (pearlPreShown) 1f
                            else EaseOutExpo.transform(seg(progress.value, 0f, 0.40f))
                            // ModulateAlpha: no offscreen buffer, so the contact
                            // shadow below the pearl isn't clipped while fading in.
                            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                            alpha = p
                            val s = 0.55f + 0.45f * p
                            scaleX = s
                            scaleY = s
                        },
                ) { drawNoirPearl(isDark) }
            }

            // Wordmark + rule hang below the pearl: top = centre + radius + gap.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight / 2 + 38.dp + 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = NoirDisplay.copy(fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = 0.01.em),
                    color = ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        val w = EaseOutExpo.transform(seg(progress.value, 0.22f, 0.56f))
                        alpha = w
                        translationY = 10.dp.toPx() * (1f - w)
                    },
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(1.dp)
                        .graphicsLayer {
                            scaleX = EaseOutExpo.transform(seg(progress.value, 0.36f, 0.66f))
                        }
                        .drawBehind { drawRect(Noir.goldHorizontal) },
                )
            }
        }
    }
}

/**
 * The Minis pearl: a nacre body with a warm champagne lobe (lower right), a
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
            0f to Color(0xFFEEE8DC),
            0.68f to Color(0xFFBFB4A2),
            1f to Color(0xFF8A8172),
            center = c,
            radius = r,
        ),
        radius = r,
        center = c,
    )
    // Warm champagne lobe
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xF2D2B88C), Color(0x00D2B88C)),
            center = at(0.72f, 0.76f),
            radius = r * 1.12f,
        ),
        radius = r,
        center = c,
    )
    // Cool silver lobe
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xCCA8B0C4), Color(0x00A8B0C4)),
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
            1f to Color(0x593C2D19),
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
