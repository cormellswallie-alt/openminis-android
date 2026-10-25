"""[T-spectra-v3] Assistant header uses the Spectra orb; typing indicator uses it too."""
import pathlib, re
root = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/chat')

# ---- AssistantHeader -------------------------------------------------------
p = root / 'ChatAssistantMessageUI.kt'
s = p.read_text()
a = s.index('        val sparkleGradient = Brush.linearGradient(\n            colors = listOf(SparkleColor1, SparkleColor2),\n        )\n        // 18.dp, matching')
b = s.index('        Spacer(modifier = Modifier.width(8.dp))\n        Text(\n            text = displayName,', a)
new_icon = '''        val sparkleGradient = Brush.linearGradient(
            colors = listOf(SparkleColor1, SparkleColor2),
        )
        // [T-spectra-v3] The Spectra orb is the assistant's face. A custom
        // Soul icon (emoji / PNG) still wins and sits inside a plain disc so
        // that user setting keeps working.
        val hasCustomIcon = soulMeta.icon.isNotBlank()
        if (hasCustomIcon) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .background(ChatColors.toolCapsuleBg, CircleShape),
            ) {
                com.openminis.app.ui.settings.SoulIconGlyph(
                    icon = soulMeta.icon,
                    sizeDp = 16.dp,
                    emojiSp = 14.sp,
                    sparkleTint = sparkleGradient,
                )
            }
        } else {
            com.openminis.app.ui.theme.SpectraOrb(
                size = 24.dp,
                state = if (live) com.openminis.app.ui.theme.OrbState.Live
                else com.openminis.app.ui.theme.OrbState.Idle,
                modifier = Modifier.padding(1.dp),
            )
        }
'''
s = s[:a] + new_icon + s[b:]
s = s.replace('internal fun AssistantHeader() {', 'internal fun AssistantHeader(live: Boolean = false) {', 1)
s = s.replace('''        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = displayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )''', '''        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = displayName,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )''', 1)
s = s.replace('            .padding(top = 10.dp, bottom = 2.dp),\n    ) {\n        val sparkleGradient',
              '            .padding(top = 14.dp, bottom = 6.dp),\n    ) {\n        val sparkleGradient', 1)
p.write_text(s)

# ---- ChatScreen: pass live to the newest header while streaming ------------
p = root / 'ChatScreen.kt'
s = p.read_text()
old = '                            is FlatChatItem.AssistantHeader -> AssistantHeader()'
assert old in s
s = s.replace(old, '''                            is FlatChatItem.AssistantHeader -> AssistantHeader(
                                live = isStreaming && item === flatItems.lastOrNull { it is FlatChatItem.AssistantHeader },
                            )''', 1)
p.write_text(s)

# ---- TypingIndicator: orb (thinking state) + calm text -------------------
p = root / 'ChatIndicators.kt'
s = p.read_text()
a = s.index('        // Breathing pearl: 1.6s in/out scale + halo, layer-only.')
b = s.index('        androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))', a)
s = s[:a] + '''        // [T-spectra-v3] Thinking orb: rotating violet ring + breathing halo.
        com.openminis.app.ui.theme.SpectraOrb(
            size = 18.dp,
            state = com.openminis.app.ui.theme.OrbState.Think,
        )
''' + s[b:]
s = s.replace('    val gold = com.openminis.app.ui.theme.Noir.Iris\n', '    val gold = com.openminis.app.ui.theme.Noir.ThinkOnPaper.let { if (isDark) com.openminis.app.ui.theme.Noir.ThinkOnOnyx else it }\n', 1)
p.write_text(s)
print('ok')
