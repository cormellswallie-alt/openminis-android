#!/usr/bin/env python3
"""Replace the two palette literals in ChatColors.kt with the Noir values.

Only the `val LightChatPalette = ChatPalette(...)` and
`val DarkChatPalette = ChatPalette(...)` blocks are rewritten; the data class,
LocalChatPalette and the ChatColors accessor are left untouched so every one
of the 276 `ChatColors.x` call sites keeps compiling.
"""
import re
import sys

PATH = sys.argv[1]
src = open(PATH, encoding="utf-8").read()

LIGHT = '''val LightChatPalette = ChatPalette(
    isDark = false,
    // [T-noir-redesign] Paper. Warm off-white page, ink text, hairlines
    // instead of fills. Champagne appears only as GoldOnPaper (thinking,
    // blockquote bar) — never as a flood.
    background = Noir.Paper,
    secondaryBg = Noir.PaperCard,
    inputBg = Noir.PaperRaised,
    inputIconBg = Noir.PaperSunk,
    inputIconBorder = Color.Transparent,
    inputBorder = Noir.PaperHairline,
    primaryText = Noir.Ink,
    secondaryText = Noir.InkSoft,
    tertiaryText = Noir.InkMute,
    disabledText = Noir.InkFaint,
    // The user's own message: solid ink with paper text — the strongest
    // object on the page, like the design boards.
    userBubble = Noir.Ink,
    userBubbleText = Noir.Paper,
    toolBg = Noir.PaperCard,
    toolBorder = Noir.PaperHairline,
    toolCapsuleBg = Noir.PaperSunk,
    separator = Noir.PaperHairline,
    sendButton = Noir.Ink,
    sendButtonDisabled = Noir.InkFaint,
    // Code stays a dark island in light mode (as before), now onyx + ivory
    // with a champagne-tinted foreground rather than terminal green.
    codeBlockBg = Color(0xFF161616),
    codeBlockText = Color(0xFFE9DFC9),
    inlineCodeBg = Noir.PaperSunk,
    inlineCodeText = Color(0xFF7A5A2C),
    link = Color(0xFF7A5A2C),
    blockquoteBar = Noir.Gold,
    thinking = Noir.GoldOnPaper,
    warningBg = Color(0x14B4442E),
    warningText = Noir.InkSoft,
    tableBorder = Noir.PaperHairlineStrong,
    inputShadow = Color(0x14141413),
    toastBg = Color(0x24CDAE74),
    thumbnailBorder = Noir.PaperHairline,
    sheetHeaderBg = Noir.PaperRaised,
    sheetHeaderBorder = Noir.PaperHairline,
    // New-chat FAB: solid ink pearl on paper.
    fabAccent = Noir.Ink,
)'''

DARK = '''val DarkChatPalette = ChatPalette(
    isDark = true,
    // [T-noir-redesign] Onyx. #0B0B0C rather than pure #000 so the lifted
    // layers (Onyx1/2/3, ~4–6% steps) stay distinguishable on ≈500-nit
    // panels — the T153 concern — while still reading as true black.
    background = Noir.Onyx,
    secondaryBg = Noir.Onyx1,
    inputBg = Noir.Onyx2,
    inputIconBg = Noir.Onyx3,
    inputIconBorder = Noir.OnyxHairline,
    inputBorder = Noir.OnyxHairlineStrong,
    primaryText = Noir.Ivory,
    secondaryText = Noir.IvorySoft,
    tertiaryText = Noir.IvoryMute,
    disabledText = Noir.IvoryFaint,
    // [T-android-user-bubble-dark-contrast] still OPAQUE and clearly lifted
    // from the background — warm graphite instead of slate blue.
    userBubble = Color(0xFF2A2825),
    userBubbleText = Noir.Ivory,
    toolBg = Noir.Onyx1,
    toolBorder = Noir.OnyxHairline,
    toolCapsuleBg = Noir.Onyx2,
    separator = Noir.OnyxHairline,
    sendButton = Noir.Ivory,
    sendButtonDisabled = Noir.IvoryFaint,
    codeBlockBg = Color(0xFF111112),
    codeBlockText = Color(0xFFE9DFC9),
    // [T-inline-code-dark-bg-android] lifted above both the wash and code
    // blocks so small chips stay visible.
    inlineCodeBg = Noir.Onyx3,
    inlineCodeText = Noir.GoldOnOnyx,
    link = Noir.GoldOnOnyx,
    blockquoteBar = Noir.Gold,
    thinking = Noir.GoldOnOnyx,
    warningBg = Color(0x1AE8826A),
    warningText = Noir.IvorySoft,
    tableBorder = Noir.OnyxHairlineStrong,
    inputShadow = Color(0x99000000),
    toastBg = Color(0x33CDAE74),
    thumbnailBorder = Noir.OnyxHairline,
    sheetHeaderBg = Noir.Onyx2,
    sheetHeaderBorder = Noir.OnyxHairline,
    // New-chat FAB: ivory pearl on onyx.
    fabAccent = Noir.Ivory,
)'''


def replace_block(text, name, new):
    start = text.index("val %s = ChatPalette(" % name)
    # find the matching close paren of this call
    i = text.index("(", start)
    depth = 0
    for j in range(i, len(text)):
        c = text[j]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return text[:start] + new + text[j + 1:]
    raise SystemExit("unbalanced block for " + name)


src = replace_block(src, "LightChatPalette", LIGHT)
src = replace_block(src, "DarkChatPalette", DARK)
open(PATH, "w", encoding="utf-8").write(src)
print("ok", src.count("Noir."), "Noir refs")
