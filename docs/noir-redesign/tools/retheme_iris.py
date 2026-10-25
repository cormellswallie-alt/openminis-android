#!/usr/bin/env python3
"""Round 3: drop champagne gold + warm paper entirely; cool neutrals with an
Iris (indigo -> violet) accent. Rewrites NoirTokens' palette block, the
Material schemes, the chat palettes, every Gold* call site and the warm
literals the first round's iOS remap introduced. Usage: retheme_iris.py <repo>"""
import os, re, sys

REPO = sys.argv[1]
UI = os.path.join(REPO, "src/android/app/src/main/java/com/openminis/app")
T = os.path.join(UI, "ui/theme")


def edit(path, pairs, count=1):
    s = open(path, encoding="utf-8").read()
    for a, b in pairs:
        n = s.count(a)
        assert n == count if count else n >= 1, (path, a[:80], n)
        s = s.replace(a, b)
    open(path, "w", encoding="utf-8").write(s)


# ── 1. tokens ─────────────────────────────────────────────────────────
p = os.path.join(T, "NoirTokens.kt")
s = open(p, encoding="utf-8").read()
a = s.index("/**\n * [T-noir-redesign] Minis Noir")
b = s.index("    /** Status hues")
s = s[:a] + '''/**
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

''' + s[b:]
a = s.index("    /** Brushed champagne")
b = s.index("}\n", s.index("val goldHorizontal")) + 2
s = s[:a] + '''    /** Iris gradient, 120°: sky → indigo → violet. */
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
''' + s[b:]
s = s.replace("    val Stone = Color(0xFF8F8A80)      // everything else",
              "    val Stone = Color(0xFF878C9C)      // everything else")
s = s.replace("never shout over the gold accents.", "never shout over the iris accent.")
open(p, "w", encoding="utf-8").write(s)

# ── 2. Material schemes ──────────────────────────────────────────────
edit(os.path.join(T, "Theme.kt"), [
    ("""    primary = Noir.Ink,
    onPrimary = Noir.Paper,
    primaryContainer = Noir.PaperSunk,
    onPrimaryContainer = Noir.Ink,
    inversePrimary = Noir.Ivory,""", """    primary = Noir.IrisDeep,
    onPrimary = Color.White,
    primaryContainer = Noir.IrisSoft,
    onPrimaryContainer = Color(0xFF1E1B6B),
    inversePrimary = Noir.IrisBright,"""),
    ("""    tertiary = Noir.GoldOnPaper,
    onTertiary = Noir.PaperRaised,
    tertiaryContainer = Color(0xFFF1E6CF),
    onTertiaryContainer = Color(0xFF3D2E14),""", """    tertiary = Noir.ThinkOnPaper,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1EBFF),
    onTertiaryContainer = Color(0xFF2E1868),"""),
    ("""    primary = Noir.Ivory,
    onPrimary = Noir.Onyx,
    primaryContainer = Noir.Onyx3,
    onPrimaryContainer = Noir.Ivory,
    inversePrimary = Noir.Ink,""", """    primary = Noir.IrisBright,
    onPrimary = Color(0xFF0E0F3A),
    primaryContainer = Noir.IrisSoftDark,
    onPrimaryContainer = Color(0xFFDCDDFF),
    inversePrimary = Noir.IrisDeep,"""),
    ("""    tertiary = Noir.GoldOnOnyx,
    onTertiary = Noir.Onyx,
    tertiaryContainer = Color(0xFF3A2F1C),
    onTertiaryContainer = Noir.GoldLight,""", """    tertiary = Noir.ThinkOnOnyx,
    onTertiary = Color(0xFF1E0F45),
    tertiaryContainer = Color(0xFF2E2350),
    onTertiaryContainer = Color(0xFFE6DCFF),"""),
    ("    error = Color(0xFFB4442E),", "    error = Color(0xFFD93A44),"),
    ("    errorContainer = Color(0xFFF6DED6),\n    onErrorContainer = Color(0xFF4A140A),",
     "    errorContainer = Color(0xFFFDE4E5),\n    onErrorContainer = Color(0xFF5A0D13),"),
    ("    error = Color(0xFFE8826A),", "    error = Color(0xFFFF7A80),"),
    ("    errorContainer = Color(0xFF3B1D16),\n    onErrorContainer = Color(0xFFF6CFC4),",
     "    errorContainer = Color(0xFF3D171B),\n    onErrorContainer = Color(0xFFFFD5D7),"),
])
s = open(os.path.join(T, "Theme.kt"), encoding="utf-8").read()
a = s.index("// Replaces the desaturated iOS-blue")
b = s.index("private val NoirLight")
s = s[:a] + """// Round 3 ("Iris"): cool neutrals, one accent.
//
//   light  page #F6F7F9, card/chat #FFFFFF, sunk #EFF1F5, ink #0F1115
//   dark   page #0D0E12, card #16171D, raised #1E2028, text #ECEEF4
//   accent Iris — #4A47D9 on paper, #8E90FF on onyx; violet for thinking
//
// `primary` is the accent now (switches, buttons, links, progress), so the
// app carries one clear hue instead of the earlier ink/ivory monochrome
// with hand-placed gold. Contrast: IrisDeep on white 6.6:1, white on
// IrisDeep 6.6:1, IrisBright on onyx 6.4:1.

""" + s[b:]
s = s.replace("is applied by hand (Noir.goldBrush)", "is applied by hand (Noir.accentBrush)")
open(os.path.join(T, "Theme.kt"), "w", encoding="utf-8").write(s)

# ── 3. chat palettes ─────────────────────────────────────────────────
edit(os.path.join(T, "ChatColors.kt"), [
    ("""    // [T-noir-redesign] Paper. Warm off-white page, ink text, hairlines
    // instead of fills. Champagne appears only as GoldOnPaper (thinking,
    // blockquote bar) — never as a flood.
    background = Noir.Paper,""", """    // [T-noir-iris] Clean white chat page, ink text, hairlines instead of
    // fills; Iris is the accent (user bubble, send, links), violet marks
    // thinking, and the step spectrum colours the tool rows.
    background = Noir.PaperCard,"""),
    ("    userBubble = Noir.Ink,\n    userBubbleText = Noir.Paper,", "    userBubble = Noir.IrisDeep,\n    userBubbleText = Color.White,"),
    ("    toolBg = Noir.PaperCard,", "    toolBg = Noir.Paper,"),
    ("    sendButton = Noir.Ink,", "    sendButton = Noir.IrisDeep,"),
    ("    codeBlockBg = Color(0xFF161616),\n    codeBlockText = Color(0xFFE9DFC9),",
     "    codeBlockBg = Color(0xFF14161C),\n    codeBlockText = Color(0xFFE3E6EE),"),
    ("    inlineCodeText = Color(0xFF7A5A2C),\n    link = Color(0xFF7A5A2C),\n    blockquoteBar = Noir.Gold,\n    thinking = Noir.GoldOnPaper,",
     "    inlineCodeText = Color(0xFFC0306A),\n    link = Noir.IrisDeep,\n    blockquoteBar = Noir.Iris,\n    thinking = Noir.ThinkOnPaper,"),
    ("    toastBg = Color(0x24CDAE74),", "    toastBg = Color(0x245B5CF0),"),
    ("    fabAccent = Noir.Ink,", "    fabAccent = Noir.IrisDeep,"),
    ("    userBubble = Color(0xFF2A2825),\n    userBubbleText = Noir.Ivory,", "    userBubble = Noir.IrisFill,\n    userBubbleText = Color.White,"),
    ("    sendButton = Noir.Ivory,", "    sendButton = Noir.IrisFill,"),
    ("    codeBlockBg = Color(0xFF111112),\n    codeBlockText = Color(0xFFE9DFC9),",
     "    codeBlockBg = Color(0xFF101116),\n    codeBlockText = Color(0xFFE3E6EE),"),
    ("    inlineCodeText = Noir.GoldOnOnyx,\n    link = Noir.GoldOnOnyx,\n    blockquoteBar = Noir.Gold,\n    thinking = Noir.GoldOnOnyx,",
     "    inlineCodeText = Color(0xFFFF8FB8),\n    link = Noir.IrisBright,\n    blockquoteBar = Noir.Iris,\n    thinking = Noir.ThinkOnOnyx,"),
    ("    toastBg = Color(0x33CDAE74),", "    toastBg = Color(0x335B5CF0),"),
    ("    fabAccent = Noir.Ivory,", "    fabAccent = Noir.IrisFill,"),
    ("    warningBg = Color(0x14B4442E),", "    warningBg = Color(0x14D93A44),"),
    ("    warningBg = Color(0x1AE8826A),", "    warningBg = Color(0x1AFF7A80),"),
])
s = open(os.path.join(T, "ChatColors.kt"), encoding="utf-8").read()
open(os.path.join(T, "ChatColors.kt"), "w", encoding="utf-8").write(s)

# ── 4. remaining Gold call sites ─────────────────────────────────────
edit(os.path.join(T, "NoirLaunch.kt"), [
    ("Noir.Gold.copy(alpha = haloAlpha * h)", "Noir.Iris.copy(alpha = haloAlpha * h)"),
    ("drawRect(Noir.goldHorizontal)", "drawRect(Noir.accentHorizontal)"),
    # pearl → cool iridescent orb
    ("            0f to Color(0xFFEEE8DC),\n            0.68f to Color(0xFFBFB4A2),\n            1f to Color(0xFF8A8172),",
     "            0f to Color(0xFFF1F2FA),\n            0.68f to Color(0xFFB9BDD6),\n            1f to Color(0xFF7D82A6),"),
    ("listOf(Color(0xF2D2B88C), Color(0x00D2B88C))", "listOf(Color(0xE69B6CF6), Color(0x009B6CF6))"),
    ("listOf(Color(0xCCA8B0C4), Color(0x00A8B0C4))", "listOf(Color(0xCC4F9BF7), Color(0x004F9BF7))"),
    ("1f to Color(0x593C2D19)", "1f to Color(0x59201C3C)"),
])
edit(os.path.join(UI, "ui/chat/ChatIndicators.kt"), [
    ("val gold = com.openminis.app.ui.theme.Noir.Gold", "val gold = com.openminis.app.ui.theme.Noir.Iris"),
])
edit(os.path.join(UI, "ui/chat/ChatStepTimeline.kt"), [
    ("internal fun thinkingInk(): Color = if (ChatColors.isDark) Noir.GoldOnOnyx else Noir.GoldOnPaper",
     "internal fun thinkingInk(): Color = if (ChatColors.isDark) Noir.ThinkOnOnyx else Noir.ThinkOnPaper"),
])

# ── 5. warm literals from round 1's iOS remap → cool, saturated-but-calm ──
MAP = {
    "9A7A48": "5B5CF0",  # was iOS blue → iris
    "C98A3E": "EE8A2A",  # orange
    "6E9C7B": "23A26D",  # green
    "5E8A6B": "1B8A5A",
    "C8553D": "E5484D",  # red
    "8F8A80": "878C9C",  # grey
    "9A7FA8": "9B6CF6",  # purple
    "7C7FA6": "6E6FE8",  # indigo
    "C2677A": "E5578A",  # pink
    "CDAE74": "F2B11E",  # yellow
    "6F9A9E": "17A2B8",  # teal
    "8A6A3A": "4A47D9",
    "E9DFC9": "E3E6EE",
    "B8B096": "8E90FF",  # sparkle gradient
    "99998C": "9B6CF6",
    "A98754": "4A47D9",
    "F3E6C8": "EDEEFF",
    "D9BF8C": "8E90FF",
    "2A2825": "1E2028",
    "E9DFC9": "E3E6EE",
}
EXCLUDE = ("ui/terminal/", "ui/theme/NoirTokens.kt")
pat = re.compile(r"Color\(0x([0-9A-Fa-f]{2})(" + "|".join(MAP) + r")\)", re.I)
pat_int = re.compile(r"0x([0-9A-Fa-f]{2})(" + "|".join(MAP) + r")(\.toInt\(\))", re.I)
total = 0
for dp, _, fs in os.walk(os.path.join(UI, "ui")):
    for f in fs:
        if not f.endswith(".kt"):
            continue
        fp = os.path.join(dp, f)
        if any(x in fp for x in EXCLUDE):
            continue
        s = open(fp, encoding="utf-8").read()
        s2, n = pat.subn(lambda m: "Color(0x%s%s)" % (m.group(1), MAP[m.group(2).upper()]), s)
        s2, n2 = pat_int.subn(lambda m: "0x%s%s%s" % (m.group(1), MAP[m.group(2).upper()], m.group(3)), s2)
        if n + n2:
            open(fp, "w", encoding="utf-8").write(s2)
            total += n + n2
print("literal swaps:", total)

# ── 6. window/splash resources ───────────────────────────────────────
res = os.path.join(REPO, "src/android/app/src/main/res")
edit(os.path.join(res, "values/colors.xml"), [("#FFF3F0E9", "#FFFFFFFF")])
edit(os.path.join(res, "values-night/colors.xml"), [("#FF0B0B0C", "#FF0D0E12")])
print("ok")
