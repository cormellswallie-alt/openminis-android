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
 * [T-spectra-v3] Minis "Spectra" — design tokens.
 *
 * Round 4. Pure white / ink, no accent hue in the chrome:
 *   • Paper  — #FFFFFF chat and cards, #F7F7F8 grouped page, #F4F4F6 sunk
 *   • Ink    — #0A0A0B text and the ONE primary fill (send, FAB, switches)
 *   • Onyx   — #0A0A0B dark ground, #151517 / #1C1C1F / #26262A lifted
 *   • Spectrum — belongs to the assistant orb ONLY (see SpectraOrb.kt).
 *     No gradient buttons, no gradient text anywhere else.
 *
 * Tool kinds keep a restrained ink each; they only tint the 30dp step node
 * and the fingerprint bar, never a fill or a glow.
 *
 * Legacy names (Paper/Ink/Onyx/Ivory/Iris…) are kept so the ~100 call sites
 * that reference them did not churn; their VALUES are the Spectra palette.
 */
object Noir {
    // ── Onyx (dark) ramp ─────────────────────────────────────────────
    val Onyx = Color(0xFF0A0A0B)
    val Onyx1 = Color(0xFF151517)      // cards / grouped rows
    val Onyx2 = Color(0xFF1C1C1F)      // elevated: sheets, menus, input
    val Onyx3 = Color(0xFF26262A)      // capsules, pressed states
    val OnyxHairline = Color(0x14FFFFFF)       // 8%
    val OnyxHairlineStrong = Color(0x24FFFFFF) // 14%

    // ── Light text on onyx (contrast ≥ 4.5 down to IvoryMute) ────────
    val Ivory = Color(0xFFF4F4F5)
    val IvorySoft = Color(0xFFCACBD0)
    val IvoryMute = Color(0xFF8F9198)
    val IvoryFaint = Color(0x52F4F4F5)

    // ── Paper (light) ramp ───────────────────────────────────────────
    val Paper = Color(0xFFF7F7F8)          // grouped page (settings, lists)
    val PaperCard = Color(0xFFFFFFFF)      // cards / chat
    val PaperRaised = Color(0xFFFFFFFF)    // sheets, input
    val PaperSunk = Color(0xFFF4F4F6)      // capsules, chips, inline code
    val PaperSunk2 = Color(0xFFECECEF)
    val PaperHairline = Color(0x120A0A0B)  // 7%
    val PaperHairlineStrong = Color(0x1F0A0A0B) // 12%

    // ── Ink text on paper (InkMute 5.2:1 on white) ───────────────────
    val Ink = Color(0xFF0A0A0B)
    val InkSoft = Color(0xFF3A3B40)
    val InkMute = Color(0xFF6B6D74)
    val InkFaint = Color(0xFFA3A5AB)

    // ── "Iris" slots are now neutral ink — the chrome has no hue ─────
    val Iris = Ink
    val IrisDeep = Ink
    val IrisBright = Ivory
    val IrisFill = Ivory
    val IrisSoft = PaperSunk
    val IrisSoftDark = Onyx3
    val Violet = Color(0xFF6E56F0)
    val Sky = Color(0xFF2563EB)

    /** Links: the only blue in the chrome, used for tappable text. */
    val LinkOnPaper = Color(0xFF2563EB)
    val LinkOnOnyx = Color(0xFF6EA8FE)

    /** Thinking ink — hairline, depth tag and caret only. */
    val ThinkOnPaper = Color(0xFF6E56F0)
    val ThinkOnOnyx = Color(0xFFB7A6FF)

    /** Status. */
    val Ok = Color(0xFF12A150)
    val OkOnOnyx = Color(0xFF3DD68C)
    val Err = Color(0xFFE5484D)
    val ErrOnOnyx = Color(0xFFFF6369)
    val Timeout = Color(0xFFE5622A)
    val Sage = Ok
    val Ember = Timeout

    /**
     * Tool kinds. Mid-lightness so one value reads on both paper and onyx
     * (toolAccentColor is not @Composable). Used at ~10% for the node fill
     * and 100% for the node glyph only.
     */
    val Moss = Color(0xFF15A673)       // shell
    val Slate = Color(0xFF3B7BEA)      // read
    val Terracotta = Color(0xFF8257F0) // write
    val Amber = Color(0xFFA24FEA)      // edit
    val Lagoon = Color(0xFF0E9DBE)     // web / browser
    val Orchid = Color(0xFF5B56EC)     // image
    val Rose = Color(0xFFDB3F86)       // memory
    val Indigo = Color(0xFF0B8FD6)     // search
    val Stone = Color(0xFF7D7F86)      // everything else

    /** Kept for source compatibility — a flat ink brush (no gradients in Spectra). */
    fun accentBrush(start: Offset = Offset.Zero, end: Offset = Offset.Infinite): Brush =
        Brush.linearGradient(listOf(Ink, Ink), start = start, end = end)

    val accentHorizontal: Brush = Brush.horizontalGradient(
        listOf(Color.Transparent, InkFaint, Color.Transparent),
    )

    /** The assistant spectrum — orb ring only. */
    val Spectrum = listOf(
        Color(0xFF8FE3D2), Color(0xFF9CBBFF), Color(0xFFC5B2FF),
        Color(0xFFF1BEEA), Color(0xFFA6E4FF), Color(0xFF8FE3D2),
    )
    val SpectrumThink = listOf(
        Color(0xFFC5B2FF), Color(0xFFF1BEEA), Color(0xFF9CBBFF),
        Color(0xFFB9A6FF), Color(0xFFC5B2FF),
    )
    val SpectrumErr = listOf(
        Color(0xFFFF8A8F), Color(0xFFF1BEEA), Color(0xFFC5B2FF), Color(0xFFFF8A8F),
    )
}

/**
 * MiSans — one family for CJK and Latin (subset: GB2312 + every glyph in the
 * zh / zh-TW string tables + Latin/Greek/Cyrillic/punctuation; anything else
 * falls back to the system face). Weights map 330/380/520/630 →
 * Normal/Medium/SemiBold/Bold.
 */
val SpectraSans: FontFamily = FontFamily(
    Font(R.font.misans_regular, FontWeight.Normal),
    Font(R.font.misans_medium, FontWeight.Medium),
    Font(R.font.misans_semibold, FontWeight.SemiBold),
    Font(R.font.misans_bold, FontWeight.Bold),
)

/** Legacy aliases — Spectra has no serif; both resolve to MiSans. */
val NoirSans: FontFamily = SpectraSans
val NoirSerif: FontFamily = SpectraSans

/** Section label ("进行中", "通用") — sentence case, no tracking games. */
val NoirEyebrow = TextStyle(
    fontFamily = SpectraSans,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    letterSpacing = 0.em,
    lineHeight = 18.sp,
)

/** Large display — home wordmark and hero titles. */
val NoirDisplay = TextStyle(
    fontFamily = SpectraSans,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.02).em,
)
