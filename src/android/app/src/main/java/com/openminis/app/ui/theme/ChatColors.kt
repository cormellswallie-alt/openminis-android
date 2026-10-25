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
    inputIconBg = Color.Transparent,
    inputIconBorder = Noir.PaperHairlineStrong,
    inputBorder = Noir.PaperHairline,
    primaryText = Noir.Ink,
    secondaryText = Noir.InkSoft,
    tertiaryText = Noir.InkMute,
    disabledText = Noir.InkFaint,
    // The user's own message: solid ink with paper text — the strongest
    // object on the page, like the design boards.
    userBubble = Noir.IrisDeep,
    userBubbleText = Color.White,
    toolBg = Noir.Paper,
    toolBorder = Noir.PaperHairline,
    toolCapsuleBg = Noir.PaperSunk,
    separator = Noir.PaperHairline,
    sendButton = Noir.IrisDeep,
    sendButtonDisabled = Noir.PaperSunk,
    // Code stays a dark island in light mode (as before), now onyx + ivory
    // with a cool light foreground rather than terminal green.
    codeBlockBg = Color(0xFF14161C),
    codeBlockText = Color(0xFFE3E6EE),
    inlineCodeBg = Noir.PaperSunk,
    inlineCodeText = Color(0xFFC0306A),
    link = Noir.IrisDeep,
    blockquoteBar = Noir.Iris,
    thinking = Noir.ThinkOnPaper,
    warningBg = Color(0x14D93A44),
    warningText = Noir.InkSoft,
    tableBorder = Noir.PaperHairlineStrong,
    inputShadow = Color(0x14141413),
    toastBg = Color(0x245B5CF0),
    thumbnailBorder = Noir.PaperHairline,
    sheetHeaderBg = Noir.PaperRaised,
    sheetHeaderBorder = Noir.PaperHairline,
    // New-chat FAB: solid ink pearl on paper.
    fabAccent = Noir.IrisDeep,
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
    inputBg = Noir.Onyx2,
    inputIconBg = Color.Transparent,
    inputIconBorder = Noir.OnyxHairlineStrong,
    inputBorder = Noir.OnyxHairlineStrong,
    primaryText = Noir.Ivory,
    secondaryText = Noir.IvorySoft,
    tertiaryText = Noir.IvoryMute,
    disabledText = Noir.IvoryFaint,
    // [T-android-user-bubble-dark-contrast] still OPAQUE and clearly lifted
    // from the background — warm graphite instead of slate blue.
    userBubble = Noir.IrisFill,
    userBubbleText = Color.White,
    toolBg = Noir.Onyx1,
    toolBorder = Noir.OnyxHairline,
    toolCapsuleBg = Noir.Onyx2,
    separator = Noir.OnyxHairline,
    sendButton = Noir.IrisFill,
    sendButtonDisabled = Noir.Onyx3,
    codeBlockBg = Color(0xFF101116),
    codeBlockText = Color(0xFFE3E6EE),
    // [T-inline-code-dark-bg-android] lifted above both the wash and code
    // blocks so small chips stay visible.
    inlineCodeBg = Noir.Onyx3,
    inlineCodeText = Color(0xFFFF8FB8),
    link = Noir.IrisBright,
    blockquoteBar = Noir.Iris,
    thinking = Noir.ThinkOnOnyx,
    warningBg = Color(0x1AFF7A80),
    warningText = Noir.IvorySoft,
    tableBorder = Noir.OnyxHairlineStrong,
    inputShadow = Color(0x99000000),
    toastBg = Color(0x335B5CF0),
    thumbnailBorder = Noir.OnyxHairline,
    sheetHeaderBg = Noir.Onyx2,
    sheetHeaderBorder = Noir.OnyxHairline,
    // New-chat FAB: ivory pearl on onyx.
    fabAccent = Noir.IrisFill,
)

val LocalChatPalette = compositionLocalOf { LightChatPalette }

// Short accessor: ChatColors.primaryText instead of LocalChatPalette.current.primaryText
val ChatColors: ChatPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalChatPalette.current
