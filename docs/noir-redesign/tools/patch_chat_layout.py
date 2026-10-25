#!/usr/bin/env python3
"""Round 3 chat layout: left-aligned two-line header with a model chip,
pill composer card with a hairline, quiet scroll buttons, avatar on the
assistant header. Behaviour untouched. Usage: patch_chat_layout.py <repo>"""
import os, sys

REPO = sys.argv[1]
CHAT = os.path.join(REPO, "src/android/app/src/main/java/com/openminis/app/ui/chat")


def edit(path, pairs):
    s = open(path, encoding="utf-8").read()
    if "[T-noir-iris-layout]" in s:
        print("already", os.path.basename(path)); return
    for a, b in pairs:
        n = s.count(a)
        assert n == 1, (os.path.basename(path), a[:90], n)
        s = s.replace(a, b)
    open(path, "w", encoding="utf-8").write(s)
    print("patched", os.path.basename(path))


edit(os.path.join(CHAT, "ChatScreen.kt"), [
    # ── header: left aligned ──
    ("""                    // iOS-style centered layout: "Minis" + group row + provider·model row
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {""",
     """                    // [T-noir-iris-layout] Left-aligned two-line header: the
                    // title, then ONE model chip (group ⌄ · provider·model ·
                    // thinking badge) instead of the old centred three-row
                    // stack. Same tap targets: title → rename sheet, chip →
                    // model picker, badge → thinking sheet.
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {"""),
    ("""                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Red.copy(alpha = 0.35f * fallbackPulseAlpha.value))""",
     """                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Red.copy(alpha = 0.35f * fallbackPulseAlpha.value))"""),
    ("""                                .padding(horizontal = 32.dp, vertical = 2.dp),
                        ) {""",
     """                                .padding(end = 8.dp, top = 2.dp, bottom = 2.dp),
                        ) {"""),
    ("""                                text = displayTitle,
                                fontSize = 16.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.SemiBold,""",
     """                                text = displayTitle,
                                fontSize = 17.sp,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.SemiBold,"""),
    ("""                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    // [T-android-modelpicker-stuck-ripple] Own the""",
     """                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .padding(start = 2.dp, top = 3.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(ChatColors.toolCapsuleBg)
                                    // [T-android-modelpicker-stuck-ripple] Own the"""),
    ("""                                    ) { showModelPicker = true }
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            ) {""",
     """                                    ) { showModelPicker = true }
                                    .padding(start = 8.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                            ) {"""),
    ("""                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        // [T-codex-fast-mode]""",
     """                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        // Shares the chip's row: yields width first.
                                        modifier = Modifier.weight(1f, fill = false),
                                    ) {
                                        // [T-codex-fast-mode]"""),
    ("                expandedHeight = 68.dp,\n", "                expandedHeight = 60.dp,\n"),
    # ── scroll buttons: quiet hairline discs ──
    ("""                            .padding(end = 12.dp + fabEndInset, bottom = upBaseBottom + 46.dp)
                            .shadow(4.dp, CircleShape)
                            .size(36.dp),
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = ChatColors.inputBg,
                            contentColor = ChatColors.primaryText,
                        ),""",
     """                            .padding(end = 12.dp + fabEndInset, bottom = upBaseBottom + 46.dp)
                            .size(34.dp)
                            .border(0.5.dp, ChatColors.inputBorder, CircleShape),
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = ChatColors.inputBg.copy(alpha = 0.94f),
                            contentColor = ChatColors.secondaryText,
                        ),"""),
    ("""                            .padding(end = 12.dp + fabEndInset, bottom = fabBottomPadding)
                            .shadow(4.dp, CircleShape)
                            .size(36.dp),
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = ChatColors.inputBg,
                            contentColor = ChatColors.primaryText,
                        ),""",
     """                            .padding(end = 12.dp + fabEndInset, bottom = fabBottomPadding)
                            .size(34.dp)
                            .border(0.5.dp, ChatColors.inputBorder, CircleShape),
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = ChatColors.inputBg.copy(alpha = 0.94f),
                            contentColor = ChatColors.secondaryText,
                        ),"""),
    # ── composer card: 24dp pill-ish card with a hairline, softer lift ──
    ("""                        .drawBehind {
                            val radiusPx = 20.dp.toPx()""",
     """                        .drawBehind {
                            val radiusPx = ComposerCardRadius.toPx()"""),
    ("""                            shadowPaint.setShadowLayer(
                                10.dp.toPx(), 0f, 3.dp.toPx(),
                                android.graphics.Color.argb(24, 0, 0, 0),
                            )""",
     """                            shadowPaint.setShadowLayer(
                                14.dp.toPx(), 0f, 4.dp.toPx(),
                                android.graphics.Color.argb(18, 20, 22, 60),
                            )"""),
    ("""                            cornerRadius = 20.dp,
                            animate = !animationsDisabled(context),
                        )""",
     """                            cornerRadius = ComposerCardRadius,
                            animate = !animationsDisabled(context),
                        )
                        // [T-noir-iris-layout] Hairline edge so the white card
                        // still reads on the white chat page.
                        .border(0.75.dp, ChatColors.inputBorder, RoundedCornerShape(ComposerCardRadius))"""),
    ("internal val SparkleColor1 =",
     "/** [T-noir-iris-layout] Composer card corner. */\ninternal val ComposerCardRadius = 24.dp\n\ninternal val SparkleColor1 ="),
])

edit(os.path.join(CHAT, "ChatAssistantMessageUI.kt"), [
    ("""        com.openminis.app.ui.settings.SoulIconGlyph(
            icon = soulMeta.icon,
            sizeDp = 18.dp,
            emojiSp = 15.sp,
            sparkleTint = sparkleGradient,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = displayName,
            fontSize = 16.sp,""",
     """        // [T-noir-iris-layout] The soul glyph sits in a soft iris disc — a
        // small avatar that marks where each reply starts.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .background(
                    if (ChatColors.isDark) com.openminis.app.ui.theme.Noir.IrisSoftDark
                    else com.openminis.app.ui.theme.Noir.IrisSoft,
                    CircleShape,
                ),
        ) {
            com.openminis.app.ui.settings.SoulIconGlyph(
                icon = soulMeta.icon,
                sizeDp = 16.dp,
                emojiSp = 14.sp,
                sparkleTint = sparkleGradient,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = displayName,
            fontSize = 15.sp,"""),
])
