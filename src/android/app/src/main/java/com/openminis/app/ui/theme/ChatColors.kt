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

// [T-claude-ui] Warm paper chat (design board v2). Light code cards, a sand
// user bubble, white composer with a warm hairline.
val LightChatPalette = ChatPalette(
    isDark = false,
    background = Noir.Paper,
    secondaryBg = Noir.PaperCard,
    inputBg = Noir.PaperRaised,
    inputIconBg = Color.Transparent,
    inputIconBorder = Noir.PaperSunk2,
    inputBorder = Noir.PaperSunk2,
    primaryText = Noir.Ink,
    secondaryText = Noir.InkSoft,
    tertiaryText = Noir.InkMute,
    disabledText = Noir.InkFaint,
    userBubble = Color(0xFFE9E5DA),
    userBubbleText = Noir.InkBody,
    toolBg = Noir.PaperRaised,
    toolBorder = Color(0xFFE9E5DF),
    toolCapsuleBg = Color(0xFFF0EEE9),
    separator = Color(0xFFE8E4DA),
    sendButton = Noir.Ink,
    sendButtonDisabled = Noir.PaperSunk2,
    codeBlockBg = Noir.PaperRaised,
    codeBlockText = Color(0xFF2F2D2A),
    inlineCodeBg = Noir.PaperSunk,
    inlineCodeText = Noir.InkBody,
    link = Noir.LinkOnPaper,
    blockquoteBar = Noir.PaperSunk2,
    thinking = Noir.ThinkOnPaper,
    warningBg = Noir.ClaySoft,
    warningText = Noir.ClayInk,
    tableBorder = Noir.PaperSunk2,
    inputShadow = Color(0x0F28241E),
    toastBg = Color(0xF01F1E1C),
    thumbnailBorder = Noir.PaperHairline,
    sheetHeaderBg = Noir.PaperCard,
    sheetHeaderBorder = Color(0xFFEFECE5),
    fabAccent = Noir.Ink,
)

val DarkChatPalette = ChatPalette(
    isDark = true,
    background = Noir.Onyx,
    secondaryBg = Noir.Onyx1,
    inputBg = Noir.Onyx2,
    inputIconBg = Color.Transparent,
    inputIconBorder = Noir.OnyxHairline,
    inputBorder = Noir.OnyxHairline,
    primaryText = Noir.Ivory,
    secondaryText = Noir.IvorySoft,
    tertiaryText = Noir.IvoryMute,
    disabledText = Noir.IvoryFaint,
    userBubble = Noir.Onyx3,
    userBubbleText = Color(0xFFE8E5DC),
    toolBg = Noir.Onyx2,
    toolBorder = Noir.OnyxHairline,
    toolCapsuleBg = Noir.Onyx3,
    separator = Noir.OnyxHairline,
    sendButton = Noir.Ivory,
    sendButtonDisabled = Noir.OnyxHairlineStrong,
    codeBlockBg = Color(0xFF2C2C2A),
    codeBlockText = Color(0xFFE8E5DC),
    inlineCodeBg = Noir.Onyx3,
    inlineCodeText = Color(0xFFE8E5DC),
    link = Noir.LinkOnOnyx,
    blockquoteBar = Noir.OnyxHairlineStrong,
    thinking = Noir.ThinkOnOnyx,
    warningBg = Noir.ClaySoftOnOnyx,
    warningText = Color(0xFFE8B8A6),
    tableBorder = Noir.OnyxHairlineStrong,
    inputShadow = Color(0x66000000),
    toastBg = Color(0xF0ECE9E1),
    thumbnailBorder = Noir.OnyxHairline,
    sheetHeaderBg = Noir.Onyx1,
    sheetHeaderBorder = Noir.OnyxHairline,
    fabAccent = Noir.Ivory,
)

val LocalChatPalette = compositionLocalOf { LightChatPalette }

// Short accessor: ChatColors.primaryText instead of LocalChatPalette.current.primaryText
val ChatColors: ChatPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalChatPalette.current
