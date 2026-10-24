package com.openminis.app.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset

/**
 * [T-noir-redesign] Minis Noir motion language.
 *
 * Principles
 *  • Short travel, long settle. Things move a little and come to rest slowly
 *    (expo-out) — the opposite of Material's big shared-axis slide.
 *  • Depth over distance. The leaving page recedes (fade + 1–2% scale-down)
 *    instead of being shoved off-screen; the arriving page drifts in ~10%.
 *  • One curve family. Everything uses [Settle] (expo-out) or [Exit]
 *    (quart-in) so the whole app feels like a single hand.
 *  • Nothing loops unless it reports live work (streaming, running tools).
 */
object NoirMotion {
    /** Expo-out: arrives fast, settles long. Default for anything entering. */
    val Settle: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /** Quart-in: clears out quickly without a visible "kick". */
    val Exit: Easing = CubicBezierEasing(0.5f, 0f, 0.75f, 0f)

    /** Gentle in-out for colour / opacity cross-fades. */
    val Ease: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    const val ENTER_MS = 420
    const val EXIT_MS = 220
    const val FADE_MS = 260

    /** Fraction of the container width a page travels when pushed. */
    private const val PUSH_TRAVEL = 0.10f
    /** Fraction a covered page drifts back (parallax) when another is pushed. */
    private const val COVER_TRAVEL = 0.06f

    fun <T> settle(ms: Int = ENTER_MS): FiniteAnimationSpec<T> = tween(ms, easing = Settle)
    fun <T> exit(ms: Int = EXIT_MS): FiniteAnimationSpec<T> = tween(ms, easing = Exit)

    /** Tactile spring for presses / toggles — quick, barely overshoots. */
    fun <T> press(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.78f, stiffness = 520f)

    // ── Page transitions (NavHost) ──────────────────────────────────────────

    /** New page arriving on push: drifts in 10% from the end edge, fades up. */
    fun pushEnter(): EnterTransition =
        slideInHorizontally(settle<IntOffset>()) { w -> (w * PUSH_TRAVEL).toInt() } +
            fadeIn(tween(FADE_MS, delayMillis = 40, easing = Ease)) +
            scaleIn(settle(), initialScale = 0.985f)

    /** Page being covered on push: recedes — drifts 6% back, dims out. */
    fun pushExit(): ExitTransition =
        slideOutHorizontally(settle<IntOffset>()) { w -> -(w * COVER_TRAVEL).toInt() } +
            fadeOut(tween(EXIT_MS, easing = Ease)) +
            scaleOut(settle(), targetScale = 0.985f)

    /** Covered page returning on back: comes forward from its parallax spot. */
    fun popEnter(): EnterTransition =
        slideInHorizontally(settle<IntOffset>()) { w -> -(w * COVER_TRAVEL).toInt() } +
            fadeIn(tween(FADE_MS, delayMillis = 40, easing = Ease)) +
            scaleIn(settle(), initialScale = 0.985f)

    /** Top page leaving on back: slides 10% toward the end edge and fades. */
    fun popExit(): ExitTransition =
        slideOutHorizontally(exit<IntOffset>()) { w -> (w * PUSH_TRAVEL).toInt() } +
            fadeOut(tween(EXIT_MS, easing = Ease))

    // ── Component-level presets ─────────────────────────────────────────────

    /** Cards / list items appearing: rise 12px and fade. */
    fun riseIn(): EnterTransition =
        slideInVertically(settle<IntOffset>()) { 24 } + fadeIn(tween(FADE_MS, easing = Ease))

    fun sinkOut(): ExitTransition =
        slideOutVertically(exit<IntOffset>()) { 16 } + fadeOut(tween(EXIT_MS, easing = Ease))

    /** Popovers / menus: grow from 94% with a fade. */
    fun popIn(): EnterTransition =
        scaleIn(settle(), initialScale = 0.94f) + fadeIn(tween(180, easing = Ease))

    fun popOut(): ExitTransition =
        scaleOut(exit(160), targetScale = 0.96f) + fadeOut(tween(140, easing = Ease))
}
