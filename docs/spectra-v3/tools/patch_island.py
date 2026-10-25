"""[T-spectra-v3] Chat top bar -> floating status island (orb + title + model pill + live chip + context ring)."""
import re, sys
P = 'src/android/app/src/main/java/com/openminis/app/ui/chat/ChatScreen.kt'
s = open(P, encoding='utf-8').read()

def once(old, new, cnt=1):
    global s
    n = s.count(old)
    if n != cnt:
        sys.exit(f'anchor count {n} != {cnt}: {old[:80]!r}')
    s = s.replace(old, new)

# 1. wrap TopAppBar in the island
m = re.search(r"        topBar = \{\n            TopAppBar\(\n                title = \{\n((?:                    //[^\n]*\n)*)                    Box\(\n                        modifier = Modifier.fillMaxWidth\(\),\n                        contentAlignment = Alignment.CenterStart,\n                    \) \{", s)
if not m: sys.exit('topbar anchor missing')
rep = ("        topBar = {\n            SpectraIsland {\n            TopAppBar(\n                title = {\n" + m.group(1) +
 "                    Row(\n                        modifier = Modifier.fillMaxWidth(),\n                        verticalAlignment = Alignment.CenterVertically,\n                    ) {\n"
 "                        // [T-spectra-v3] The assistant's face leads the island.\n"
 "                        com.openminis.app.ui.theme.SpectraOrb(\n                            size = 30.dp,\n"
 "                            state = if (isStreaming) com.openminis.app.ui.theme.OrbState.Live else com.openminis.app.ui.theme.OrbState.Idle,\n"
 "                            modifier = Modifier.padding(end = 8.dp),\n                        )\n"
 "                        Box(\n                        modifier = Modifier.weight(1f),\n                        contentAlignment = Alignment.CenterStart,\n                    ) {")
s = s[:m.start()] + rep + s[m.end():]

# 2. close: insert live chip after title box; locate the island end by the unique colors block
old_tail = """                windowInsets = WindowInsets.statusBars,
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = ChatColors.background.copy(alpha = 0.92f),
                    scrolledContainerColor = ChatColors.background.copy(alpha = 0.92f),
                ),"""
new_tail = """                windowInsets = WindowInsets(0),
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),"""
once(old_tail, new_tail)
# find "expandedHeight = 60.dp,\n            )\n        }," right after
once("                expandedHeight = 60.dp,\n            )\n        },\n        snackbarHost",
     "                expandedHeight = 62.dp,\n            )\n            }\n        },\n        snackbarHost")

# 3. title Box closes before `},\n                navigationIcon` or `actions`. Insert chip after the Box.
i = s.index("SpectraIsland {")
j = s.index("                actions = {", i)
# the title lambda ends with "\n                    }\n                },\n" just before navigationIcon/actions
k = s.rindex("\n                },\n", i, j)
# find which slot comes first after title
nav = s.find("                navigationIcon = {", i, j)
end_title = s.index("\n                },\n", i)  # first lambda close after island start == title close
seg = s[i:end_title]
# title close: "...\n                    }" (Row) — we opened Row + inner Box; inner Box replaced old Box, so the old
# Box closing brace now closes the inner Box; add chip + Row close.
assert seg.endswith("\n                    }"), seg[-60:]
s = s[:end_title] + "\n                        SpectraLiveChip(isStreaming = isStreaming, modifier = Modifier.padding(start = 6.dp))\n                    }" + s[end_title:]

# 4. context ring in actions, before the overflow menu
once("                actions = {\n                    // iOS: \"...\" circle button → dropdown menu\n                    Box {\n                        IconButton(onClick = { viewModel.refreshEnhancedCacheState(); showChatMenu = true }) {",
     "                actions = {\n                    contextUsage?.let { cu ->\n                        if (cu.windowTokens > 0) SpectraContextRing(percent = cu.percent, onClick = { showTokenUsageSheet = true })\n                    }\n                    // iOS: \"...\" circle button → dropdown menu\n                    Box {\n                        IconButton(onClick = { viewModel.refreshEnhancedCacheState(); showChatMenu = true }) {")
open(P, 'w', encoding='utf-8').write(s)
print('ok')
