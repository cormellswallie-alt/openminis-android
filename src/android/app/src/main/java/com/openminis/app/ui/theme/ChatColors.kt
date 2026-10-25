package com.openminis.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Semantic chat colors mirroring iOS ChatColors (AIChatView.swift).
// Resolved from LocalChatPalette, which is provided by MinisTheme.
//
// iOS reference:
//   systemBackground        -> background
//   secondarySystemBackground -> secondaryBg
//   tertiarySystemFill      -> userBubble
//   tertiarySystemGroupedBackground -> toolBg
//   label                   -> primaryText
//   secondaryLabel          -> secondaryText
//   tertiaryLabel           -> tertiaryText
//   quaternaryLabel         -> sendButtonDisabled
//   separator               -> border
//   systemGray6             -> inlineCodeBg / toolCapsuleBg
@Immutable
data class ChatPalette(
    val isDark: Boolean,
    val background: Color,
    val secondaryBg: Color,
    val inputBg: Color,
    val inputIconBg: Color,
    val inputIconBorder: Color,
    val inputBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val disabledText: Color,
    val userBubble: Color,
    /** [T-noir-redesign] Text drawn ON [userBubble] (paper on ink in light mode). */
    val userBubbleText: Color,
    val toolBg: Color,
    val toolBorder: Color,
    val toolCapsuleBg: Color,
    val separator: Color,
    val sendButton: Color,
    val sendButtonDisabled: Color,
    val codeBlockBg: Color,
    val codeBlockText: Color,
    val inlineCodeBg: Color,
    val inlineCodeText: Color,
    val link: Color,
    val blockquoteBar: Color,
    val thinking: Color,
    val warningBg: Color,
    val warningText: Color,
    val tableBorder: Color,
    val inputShadow: Color,
    val toastBg: Color,
    val thumbnailBorder: Color,
    val sheetHeaderBg: Color,
    val sheetHeaderBorder: Color,
    val fabAccent: Color,
)

// [T-spectra-v3] Pure white chat, ink text and ink primary. The user's turn
// has no bubble (it is the page heading), so userBubble only backs legacy
// surfaces such as the edit-in-place field.
val LightChatPalette = ChatPalette(
    isDark = false,
    // [T-noir-iris] Clean white chat page, ink text, hairlines instead of
    // fills; Iris is the accent (user bubble, send, links), violet marks
    // thinking, and the step spectrum colours the tool rows.
    background = Noir.PaperCard,
    secondaryBg = Noir.PaperCard,
    inputBg = Noir.PaperRaised,
    // [T-noir-step-timeline] Composer tools are hairline rings, not beige
    // discs — the fills were half of why the chat read as one flat beige.
    inputIconBg = Noir.PaperSunk,
    inputIconBorder = Color.Transparent,
    inputBorder = Noir.PaperHairline,
    primaryText = Noir.Ink,
    secondaryText = Noir.InkSoft,
    tertiaryText = Noir.InkMute,
    disabledText = Noir.InkFaint,
    // The user's own message: solid ink with paper text — the strongest
    // object on the page, like the design boards.
    userBubble = Noir.PaperSunk,
    userBubbleText = Noir.Ink,
    toolBg = Noir.Paper,
    toolBorder = Noir.PaperHairline,
    toolCapsuleBg = Noir.PaperSunk,
    separator = Noir.PaperHairline,
    sendButton = Noir.Ink,
    sendButtonDisabled = Noir.PaperSunk2,
    // Code stays a dark island in light mode (as before), now onyx + ivory
    // with a cool light foreground rather than terminal green.
    codeBlockBg = Color(0xFF111113),
    codeBlockText = Color(0xFFE4E4E7),
    inlineCodeBg = Noir.PaperSunk,
    inlineCodeText = Noir.InkSoft,
    link = Noir.LinkOnPaper,
    blockquoteBar = Color(0xFFDCDCE0),
    thinking = Noir.ThinkOnPaper,
    warningBg = Color(0x12E5484D),
    warningText = Noir.InkSoft,
    tableBorder = Noir.PaperHairlineStrong,
    inputShadow = Color(0x1A0A0A0B),
    toastBg = Color(0xF00A0A0B),
    thumbnailBorder = Noir.PaperHairline,
    sheetHeaderBg = Noir.PaperRaised,
    sheetHeaderBorder = Noir.PaperHairline,
    // New-chat FAB: solid ink pearl on paper.
    fabAccent = Noir.Ink,
)

// T153: Android-specific dark palette tweaks. iOS borrows the system
// palette (#1C1C1E / #2C2C2E etc.) which reads as "layered dark grey"
// on a 1000+ nit display, but on a typical Android phone (Pixel 6 ≈
// 500 nits, mid-range OEMs even less) those layers crush together
// into a single near-black wash and the user can't tell tool capsules
// from background or input from message list. Lift the non-background
// layers ~6-10% so the contrast survives the brightness gap; the pure
// `background` itself stays #000 because every other color is keyed
// to "darker than this".
val DarkChatPalette = ChatPalette(
    isDark = true,
    // [T-noir-redesign] Onyx. #0B0B0C rather than pure #000 so the lifted
    // layers (Onyx1/2/3, ~4–6% steps) stay distinguishable on ≈500-nit
    // panels — the T153 concern — while still reading as true black.
    background = Noir.Onyx,
    secondaryBg = Noir.Onyx1,
    inputBg = Noir.Onyx1,
    inputIconBg = Noir.Onyx3,
    inputIconBorder = Color.Transparent,
    inputBorder = Noir.OnyxHairline,
    primaryText = Noir.Ivory,
    secondaryText = Noir.IvorySoft,
    tertiaryText = Noir.IvoryMute,
    disabledText = Noir.IvoryFaint,
    // [T-android-user-bubble-dark-contrast] still OPAQUE and clearly lifted
    // from the background — warm graphite instead of slate blue.
    userBubble = Noir.Onyx2,
    userBubbleText = Noir.Ivory,
    toolBg = Noir.Onyx1,
    toolBorder = Noir.OnyxHairline,
    toolCapsuleBg = Noir.Onyx2,
    separator = Noir.OnyxHairline,
    sendButton = Noir.Ivory,
    sendButtonDisabled = Noir.Onyx3,
    codeBlockBg = Color(0xFF111113),
    codeBlockText = Color(0xFFE4E4E7),
    // [T-inline-code-dark-bg-android] lifted above both the wash and code
    // blocks so small chips stay visible.
    inlineCodeBg = Noir.Onyx3,
    inlineCodeText = Noir.IvorySoft,
    link = Noir.LinkOnOnyx,
    blockquoteBar = Color(0xFF3A3A40),
    thinking = Noir.ThinkOnOnyx,
    warningBg = Color(0x1AFF6369),
    warningText = Noir.IvorySoft,
    tableBorder = Noir.OnyxHairlineStrong,
    inputShadow = Color(0x99000000),
    toastBg = Color(0xF0F4F4F5),
    thumbnailBorder = Noir.OnyxHairline,
    sheetHeaderBg = Noir.Onyx2,
    sheetHeaderBorder = Noir.OnyxHairline,
    // New-chat FAB: ivory pearl on onyx.
    fabAccent = Noir.Ivory,
)

val LocalChatPalette = compositionLocalOf { LightChatPalette }

// Short accessor: ChatColors.primaryText instead of LocalChatPalette.current.primaryText
val ChatColors: ChatPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalChatPalette.current
