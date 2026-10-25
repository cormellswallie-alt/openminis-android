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
 * Round 3 ("Iris"): the champagne-gold + warm-paper pairing read like a
 * night-light filter on a real panel, so warmth is gone. Materials now:
 *   • Onyx   — cool graphite ground (#0D0E12) and its lifted layers
 *   • Paper  — clean cool white (#F6F7F9 page, #FFFFFF cards/chat)
 *   • Iris   — indigo → violet accent. Solid for primary actions, the user
 *              bubble and links; the gradient only for brand moments
 *              (launch halo, send button, thinking node).
 *
 * The step spectrum (below) supplies the per-tool colour, so the chat is
 * never a single-hue wall. Legacy names (Paper/Ink/Onyx/Ivory) are kept so
 * 100+ call sites did not churn — they are cool neutrals now.
 */
object Noir {
    // ── Onyx (dark) ramp — cool graphite ─────────────────────────────
    // Lifted in ~4–6% steps: on a ≈500-nit Android panel smaller steps
    // crush together (see the T153 note in ChatColors.kt).
    val Onyx = Color(0xFF0D0E12)
    val Onyx1 = Color(0xFF16171D)      // cards / grouped rows
    val Onyx2 = Color(0xFF1E2028)      // elevated: sheets, menus, input
    val Onyx3 = Color(0xFF282B35)      // capsules, pressed states
    val OnyxHairline = Color(0x1AE8EAF2)       // 10%
    val OnyxHairlineStrong = Color(0x2BE8EAF2) // 17%

    // ── Light text on onyx ────────────────────────────────────────────
    val Ivory = Color(0xFFECEEF4)
    val IvorySoft = Color(0xFFB3B8C6)
    val IvoryMute = Color(0xFF7E8496)
    val IvoryFaint = Color(0x4DECEEF4)

    // ── Paper (light) ramp — neutral cool white ──────────────────────
    val Paper = Color(0xFFF6F7F9)          // page (settings, lists)
    val PaperCard = Color(0xFFFFFFFF)      // cards / grouped rows / chat
    val PaperRaised = Color(0xFFFFFFFF)    // sheets, input
    val PaperSunk = Color(0xFFEFF1F5)      // capsules, chips, inline code
    val PaperHairline = Color(0x170F1115)  // 9%
    val PaperHairlineStrong = Color(0x290F1115)

    // ── Ink text on paper ─────────────────────────────────────────────
    val Ink = Color(0xFF0F1115)
    val InkSoft = Color(0xFF4A5060)
    val InkMute = Color(0xFF878C9C)
    val InkFaint = Color(0x4D0F1115)

    // ── Iris accent ───────────────────────────────────────────────────
    val Iris = Color(0xFF5B5CF0)            // brand mid-tone
    val IrisDeep = Color(0xFF4A47D9)        // text / icons / fills on paper
    val IrisBright = Color(0xFF8E90FF)      // text / icons on onyx
    val IrisFill = Color(0xFF5E5FEA)        // solid fill (bubble, send) in dark
    val IrisSoft = Color(0xFFEDEEFF)        // container on paper
    val IrisSoftDark = Color(0xFF242650)    // container on onyx
    val Violet = Color(0xFF9B6CF6)
    val Sky = Color(0xFF4F9BF7)
    /** Thinking ink: violet, distinct from the indigo of actions. */
    val ThinkOnPaper = Color(0xFF7A4FD8)
    val ThinkOnOnyx = Color(0xFFB79CFF)

    /** Status hues, desaturated to sit inside the palette. */
    val Sage = Color(0xFF7FA88B)
    val Ember = Color(0xFFD9775B)

    /**
     * Step spectrum. Gold-on-ink alone read as monotone once a turn ran a
     * dozen tools, so every tool kind gets its own muted ink. All nine sit
     * at similar lightness/chroma, so they read as one family on both Paper
     * and Onyx and never shout over the iris accent.
     */
    val Moss = Color(0xFF5E9466)       // shell
    val Slate = Color(0xFF5B83B0)      // read
    val Terracotta = Color(0xFFC2714F) // write
    val Amber = Color(0xFFC9902F)      // edit
    val Lagoon = Color(0xFF3E9490)     // web / browser
    val Orchid = Color(0xFF9C6BB0)     // image
    val Rose = Color(0xFFC75D7B)       // memory
    val Indigo = Color(0xFF6C72C4)     // search
    val Stone = Color(0xFF878C9C)      // everything else

    /** Iris gradient, 120°: sky → indigo → violet. */
    fun accentBrush(start: Offset = Offset.Zero, end: Offset = Offset.Infinite): Brush =
        Brush.linearGradient(
            0f to Sky,
            0.5f to Iris,
            1f to Violet,
            start = start,
            end = end,
        )

    val accentHorizontal: Brush = Brush.horizontalGradient(
        listOf(Sky.copy(alpha = 0f), Sky, Iris, Violet, Violet.copy(alpha = 0f)),
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
