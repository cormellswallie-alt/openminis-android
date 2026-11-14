package com.openminis.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material 3 Expressive semantic chat & surface tokens.
 *
 * Harmonized with [ColorScheme] (including Android 12+ Material You dynamic colors)
 * so chat bubbles, command decks, tool capsules, and markdown code blocks share
 * one cohesive M3 Expressive tonal hierarchy instead of flat iOS system grays.
 */
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
    val onUserBubble: Color = primaryText,
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
    background = Color(0xFFF6F7FC),
    secondaryBg = Color(0xFFEAEDF7),
    inputBg = Color(0xFFFFFFFF),
    inputIconBg = Color(0xFFEAEDF7),
    inputIconBorder = Color.Transparent,
    inputBorder = Color(0x28434753),
    primaryText = Color(0xFF171B23),
    secondaryText = Color(0xFF434753),
    tertiaryText = Color(0xFF737785),
    disabledText = Color(0x61171B23),
    userBubble = Color(0xFFDCE3FF),
    onUserBubble = Color(0xFF00164E),
    toolBg = Color(0xFFEAEDF7),
    toolBorder = Color(0x26737785),
    toolCapsuleBg = Color(0xFFE3E7F3),
    separator = Color(0x2E737785),
    sendButton = Color(0xFF2956D8),
    sendButtonDisabled = Color(0x38171B23),
    codeBlockBg = Color(0xFF121620),
    codeBlockText = Color(0xFF8BE9B4),
    inlineCodeBg = Color(0xFFE3E7F3),
    inlineCodeText = Color(0xFF8C4168),
    link = Color(0xFF2956D8),
    blockquoteBar = Color(0x992956D8),
    thinking = Color(0xFF8C4168),
    warningBg = Color(0x1FD97706),
    warningText = Color(0xFF7C4A03),
    tableBorder = Color(0x28434753),
    inputShadow = Color(0x14171B23),
    toastBg = Color(0xFFDCE3FF),
    thumbnailBorder = Color(0x28434753),
    sheetHeaderBg = Color(0xFFF0F2FA),
    sheetHeaderBorder = Color(0x1F737785),
    fabAccent = Color(0xFF2956D8),
)

val DarkChatPalette = ChatPalette(
    isDark = true,
    background = Color(0xFF0F131B),
    secondaryBg = Color(0xFF1D222D),
    inputBg = Color(0xFF232937),
    inputIconBg = Color(0xFF2F3647),
    inputIconBorder = Color(0x338D91A0),
    inputBorder = Color(0x388D91A0),
    primaryText = Color(0xFFE2E5F0),
    secondaryText = Color(0xFFC3C6D6),
    tertiaryText = Color(0xFF8D91A0),
    disabledText = Color(0x52E2E5F0),
    userBubble = Color(0xFF203E9A),
    onUserBubble = Color(0xFFE3E8FF),
    toolBg = Color(0xFF1D222D),
    toolBorder = Color(0x338D91A0),
    toolCapsuleBg = Color(0xFF272C3A),
    separator = Color(0x388D91A0),
    sendButton = Color(0xFFB5C4FF),
    sendButtonDisabled = Color(0x38E2E5F0),
    codeBlockBg = Color(0xFF0A0D14),
    codeBlockText = Color(0xFF8CF3B5),
    inlineCodeBg = Color(0xFF272C3A),
    inlineCodeText = Color(0xFFFFB0D0),
    link = Color(0xFFB5C4FF),
    blockquoteBar = Color(0x99B5C4FF),
    thinking = Color(0xFFFFB0D0),
    warningBg = Color(0x24F59E0B),
    warningText = Color(0xFFFCD34D),
    tableBorder = Color(0x388D91A0),
    inputShadow = Color(0x73000000),
    toastBg = Color(0xFF203E9A),
    thumbnailBorder = Color(0x388D91A0),
    sheetHeaderBg = Color(0xFF171B24),
    sheetHeaderBorder = Color(0x298D91A0),
    fabAccent = Color(0xFF1A40B0),
)

/**
 * Derives a cohesive [ChatPalette] from the active Material 3 [ColorScheme]
 * so both the signature Expressive Cobalt/Iris theme and Android 12+ Dynamic Color
 * produce harmonious chat bubbles, tool pills, and command decks.
 */
fun expressiveChatPalette(scheme: ColorScheme, isDark: Boolean): ChatPalette {
    val base = if (isDark) DarkChatPalette else LightChatPalette
    return base.copy(
        background = scheme.background,
        secondaryBg = scheme.surfaceContainer,
        inputBg = if (isDark) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest,
        inputIconBg = scheme.surfaceContainerHighest,
        inputBorder = scheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.55f),
        primaryText = scheme.onSurface,
        secondaryText = scheme.onSurfaceVariant,
        tertiaryText = scheme.outline,
        disabledText = scheme.onSurface.copy(alpha = 0.36f),
        userBubble = scheme.primaryContainer,
        onUserBubble = scheme.onPrimaryContainer,
        toolBg = scheme.surfaceContainerLow,
        toolBorder = scheme.outlineVariant.copy(alpha = 0.45f),
        toolCapsuleBg = scheme.surfaceContainerHigh,
        separator = scheme.outlineVariant.copy(alpha = 0.5f),
        sendButton = scheme.primary,
        sendButtonDisabled = scheme.onSurface.copy(alpha = 0.22f),
        inlineCodeBg = scheme.surfaceContainerHigh,
        inlineCodeText = scheme.tertiary,
        link = scheme.primary,
        blockquoteBar = scheme.primary.copy(alpha = 0.65f),
        thinking = scheme.tertiary,
        tableBorder = scheme.outlineVariant.copy(alpha = 0.55f),
        toastBg = scheme.primaryContainer,
        thumbnailBorder = scheme.outlineVariant.copy(alpha = 0.5f),
        sheetHeaderBg = scheme.surfaceContainerLow,
        sheetHeaderBorder = scheme.outlineVariant.copy(alpha = 0.35f),
        fabAccent = scheme.primaryContainer,
    )
}

val LocalChatPalette = compositionLocalOf { LightChatPalette }

val ChatColors: ChatPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalChatPalette.current
