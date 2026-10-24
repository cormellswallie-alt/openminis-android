package com.openminis.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.openminis.app.R

/**
 * [T-noir-redesign] Minis Noir — the design language tokens.
 *
 * Three materials and nothing else:
 *   • Onyx      — the dark ground (#0B0B0C) and its lifted layers
 *   • Ivory     — warm paper (#F3F0E9) for light mode, warm ivory text on onyx
 *   • Champagne — a brushed-gold gradient used *sparingly*: active indicators,
 *                 progress, the pearl's glow, the launch wordmark rule
 *
 * Colour is deliberately almost absent. Hierarchy comes from typography
 * (Instrument Serif display over Inter text), hairlines (0.5–1dp at 7–15%
 * alpha) and generous spacing, not from fills or saturated accents.
 *
 * Every value lives here so Theme.kt / ChatColors.kt / the launch overlay
 * and the Noir components read one source of truth.
 */
object Noir {
    // ── Onyx (dark) ramp ──────────────────────────────────────────────
    // Lifted in ~4–6% steps: on a ≈500-nit Android panel smaller steps
    // crush together (see the T153 note in ChatColors.kt).
    val Onyx = Color(0xFF0B0B0C)
    val Onyx1 = Color(0xFF151516)      // cards / grouped rows
    val Onyx2 = Color(0xFF1D1D1F)      // elevated: sheets, menus, input
    val Onyx3 = Color(0xFF262628)      // tool capsules, pressed states
    val OnyxHairline = Color(0x1AEFEBE3)   // ivory @ 10%
    val OnyxHairlineStrong = Color(0x29EFEBE3) // ivory @ 16%

    // ── Ivory text on onyx ────────────────────────────────────────────
    val Ivory = Color(0xFFEFEBE3)
    val IvorySoft = Color(0xFFBDB8AE)
    val IvoryMute = Color(0xFF85817A)
    val IvoryFaint = Color(0x4DEFEBE3)

    // ── Paper (light) ramp ────────────────────────────────────────────
    val Paper = Color(0xFFF3F0E9)          // page
    val PaperCard = Color(0xFFFBFAF6)      // cards / grouped rows
    val PaperRaised = Color(0xFFFFFFFF)    // sheets, input
    val PaperSunk = Color(0xFFE9E5DC)      // capsules, chips, inline code
    val PaperHairline = Color(0x1A141413)  // ink @ 10%
    val PaperHairlineStrong = Color(0x2B141413)

    // ── Ink text on paper ─────────────────────────────────────────────
    val Ink = Color(0xFF141413)
    val InkSoft = Color(0xFF4A4842)
    val InkMute = Color(0xFF8F8A80)
    val InkFaint = Color(0x4D141413)

    // ── Champagne ─────────────────────────────────────────────────────
    val GoldLight = Color(0xFFF3E6C8)
    val Gold = Color(0xFFCDAE74)
    val GoldSheen = Color(0xFFEAD8B0)
    val GoldDeep = Color(0xFFA98754)
    /** Readable gold for text/icons: deep bronze on paper, champagne on onyx. */
    val GoldOnPaper = Color(0xFF8A6A3A)
    val GoldOnOnyx = Color(0xFFD9BF8C)

    /** Status hues, desaturated to sit inside the palette. */
    val Sage = Color(0xFF7FA88B)
    val Ember = Color(0xFFD9775B)

    /** Brushed champagne, 115° like the design boards. */
    fun goldBrush(start: Offset = Offset.Zero, end: Offset = Offset.Infinite): Brush =
        Brush.linearGradient(
            0f to GoldLight,
            0.38f to Gold,
            0.60f to GoldSheen,
            1f to GoldDeep,
            start = start,
            end = end,
        )

    val goldHorizontal: Brush = Brush.horizontalGradient(
        listOf(GoldDeep.copy(alpha = 0f), Gold, GoldLight, Gold, GoldDeep.copy(alpha = 0f)),
    )
}

/** Instrument Serif — display / titles. Latin-only; CJK falls back to the system face. */
val NoirSerif: FontFamily = FontFamily(
    Font(R.font.noir_serif_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.noir_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Inter — all running text and UI labels. CJK falls back to the system face. */
val NoirSans: FontFamily = FontFamily(
    Font(R.font.noir_inter_regular, FontWeight.Normal),
    Font(R.font.noir_inter_medium, FontWeight.Medium),
    Font(R.font.noir_inter_semibold, FontWeight.SemiBold),
    Font(R.font.noir_inter_bold, FontWeight.Bold),
)

/** Small-caps style eyebrow label ("IN PROGRESS", "GENERAL"). */
val NoirEyebrow = TextStyle(
    fontFamily = NoirSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 0.18.em,
    lineHeight = 14.sp,
)

/** Serif display used for the home wordmark and hero titles. */
val NoirDisplay = TextStyle(
    fontFamily = NoirSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 34.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.01).em,
)
