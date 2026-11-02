package com.openminis.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.openminis.app.R

/**
 * [T-claude-ui] OpenMinis "Claude" design tokens (design board v2, rounds 2–12).
 *
 *   • Paper  — #F5F4ED page (chat, settings, drawers), #FBFAF6 groups/sheets,
 *              #FFFFFF composer and cards, #EBE8DE tiles, #E9E5DA user bubble
 *   • Ink    — #1F1E1C primary fill (send, switches), #2A2825 prose
 *   • Onyx   — #262624 dark page, #2C2C2A sheet, #30302E card, #3A3936 tile
 *   • Accent — terracotta #C96442 (#D97757 on onyx), used sparingly
 *
 * Legacy names (Paper/Ink/Onyx/Ivory/Iris…) are kept so the ~100 call sites
 * that reference them did not churn; their VALUES are the Claude palette.
 */
object Noir {
    // ── Onyx (dark) ramp ─────────────────────────────────────────────
    val Onyx = Color(0xFF262624)
    val Onyx1 = Color(0xFF2C2C2A)      // sheets / grouped rows
    val Onyx2 = Color(0xFF30302E)      // cards, composer, menus
    val Onyx3 = Color(0xFF3A3936)      // tiles, bubble, pressed
    val OnyxHairline = Color(0xFF3E3D39)
    val OnyxHairlineStrong = Color(0xFF4A4844)

    // ── Light text on onyx ───────────────────────────────────────────
    val Ivory = Color(0xFFECE9E1)
    val IvorySoft = Color(0xFFC2BFB6)
    val IvoryMute = Color(0xFF8F8B82)
    val IvoryFaint = Color(0xFF6B6862)

    // ── Paper (light) ramp ───────────────────────────────────────────
    val Paper = Color(0xFFF5F4ED)          // page
    val PaperCard = Color(0xFFFBFAF6)      // groups, sheets, menus
    val PaperRaised = Color(0xFFFFFFFF)    // composer, white cards
    val PaperSunk = Color(0xFFEBE8DE)      // tiles, chips, inline code
    val PaperSunk2 = Color(0xFFE2DED3)
    val PaperHairline = Color(0xFFE6E2DB)
    val PaperHairlineStrong = Color(0xFFE2DED3)

    // ── Ink text on paper ────────────────────────────────────────────
    val Ink = Color(0xFF1F1E1C)
    val InkBody = Color(0xFF2A2825)
    val InkSoft = Color(0xFF3D3A35)
    val InkThought = Color(0xFF6B6862)
    val InkMute = Color(0xFF8A867F)
    val InkMeta = Color(0xFF9A968E)
    val InkFaint = Color(0xFFB5B0A7)

    /** Terracotta — the one warm accent. */
    val Clay = Color(0xFFC96442)
    val ClayOnOnyx = Color(0xFFD97757)
    val ClaySoft = Color(0xFFF3E4DA)
    val ClaySoftOnOnyx = Color(0xFF3D2A25)
    val ClayInk = Color(0xFF7A3F2A)

    /** Brand mark (app avatar). */
    val Brand = Color(0xFF3F5F52)
    val BrandOnOnyx = Color(0xFF7FB59C)

    // ── "Iris" slots are neutral ink — the chrome has no hue ─────────
    val Iris = Ink
    val IrisDeep = Ink
    val IrisBright = Ivory
    val IrisFill = Ivory
    val IrisSoft = PaperSunk
    val IrisSoftDark = Onyx3
    val Violet = Color(0xFF6B53B0)
    val Sky = Color(0xFF3D6FA3)

    /** Links. */
    val LinkOnPaper = Color(0xFFC96442)
    val LinkOnOnyx = Color(0xFFD97757)

    /** Thinking ink — summaries, step bodies. */
    val ThinkOnPaper = Color(0xFF6B6862)
    val ThinkOnOnyx = Color(0xFFA9A69D)

    /** Status. */
    val Ok = Color(0xFF4F8A72)
    val OkOnOnyx = Color(0xFF7FB59C)
    val Err = Color(0xFFB5523F)
    val ErrOnOnyx = Color(0xFFD98572)
    val Timeout = Color(0xFFB5703F)
    val Sage = Ok
    val Ember = Timeout

    /**
     * Tool kinds — muted earth tones; the design keeps step glyphs grey, so
     * these only ever tint a node at low alpha.
     */
    val Moss = Color(0xFF4F8A72)       // shell
    val Slate = Color(0xFF3D6FA3)      // read
    val Terracotta = Color(0xFFC96442) // write
    val Amber = Color(0xFFB08A3E)      // edit
    val Lagoon = Color(0xFF3F7F86)     // web / browser
    val Orchid = Color(0xFF6B53B0)     // image
    val Rose = Color(0xFFB5527A)       // memory
    val Indigo = Color(0xFF4E6A9E)     // search
    val Stone = Color(0xFF8A867F)      // everything else

    /** Kept for source compatibility — a flat ink brush. */
    fun accentBrush(start: Offset = Offset.Zero, end: Offset = Offset.Infinite): Brush =
        Brush.linearGradient(listOf(Ink, Ink), start = start, end = end)

    val accentHorizontal: Brush = Brush.horizontalGradient(
        listOf(Color.Transparent, InkFaint, Color.Transparent),
    )

    /** Assistant orb ring — warm clay / sand. */
    val Spectrum = listOf(
        Color(0xFFD97757), Color(0xFFE8B48A), Color(0xFFC96442),
        Color(0xFFB49A74), Color(0xFFE09A73), Color(0xFFD97757),
    )
    val SpectrumThink = listOf(
        Color(0xFFB49A74), Color(0xFFD97757), Color(0xFFCFC3AE),
        Color(0xFFC96442), Color(0xFFB49A74),
    )
    val SpectrumErr = listOf(
        Color(0xFFB5523F), Color(0xFFD98572), Color(0xFFC96442), Color(0xFFB5523F),
    )

    /** Max-depth flowing gradient (depth chip / slider only). */
    val MaxGradient = listOf(
        Color(0xFFF4A259), Color(0xFFEF6F5E), Color(0xFFDE4F8C), Color(0xFF9A63E8),
        Color(0xFFDE4F8C), Color(0xFFEF6F5E), Color(0xFFF4A259),
    )
}

/**
 * Geist (Latin) — CJK falls back to the system Noto Sans CJK, which is the
 * design's "Geist, Noto Sans SC" stack.
 */
val SpectraSans: FontFamily = FontFamily(
    Font(R.font.om_sans_regular, FontWeight.Normal),
    Font(R.font.om_sans_medium, FontWeight.Medium),
    Font(R.font.om_sans_semibold, FontWeight.SemiBold),
    Font(R.font.om_sans_bold, FontWeight.Bold),
)

/**
 * Noto Serif SC (its Latin is the Source Serif design) — assistant prose,
 * sheet titles and display numbers: "Source Serif 4, Noto Serif SC".
 */
val ClaudeSerif: FontFamily = FontFamily(
    Font(R.font.om_serif_regular, FontWeight.Normal),
    Font(R.font.om_serif_semibold, FontWeight.SemiBold),
    Font(R.font.om_serif_semibold, FontWeight.Bold),
)

/** Geist Mono — code, commands, model ids. */
val ClaudeMono: FontFamily = FontFamily(
    Font(R.font.om_mono_regular, FontWeight.Normal),
    Font(R.font.om_mono_medium, FontWeight.Medium),
)

val NoirSans: FontFamily = SpectraSans
val NoirSerif: FontFamily = ClaudeSerif

/** Section label ("进行中", "通用") — sentence case, no tracking games. */
val NoirEyebrow = TextStyle(
    fontFamily = SpectraSans,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    letterSpacing = 0.em,
    lineHeight = 18.sp,
)

/** Large display — serif titles. */
val NoirDisplay = TextStyle(
    fontFamily = ClaudeSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.01).em,
)
