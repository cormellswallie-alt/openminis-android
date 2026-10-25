#!/usr/bin/env python3
"""[T-spectra-v3-composer] Move model switch + thinking depth into the composer,
and slim the chat header down to orb + title (+ live status)."""
import pathlib, sys

P = pathlib.Path("src/android/app/src/main/java/com/openminis/app/ui/chat/ChatScreen.kt")
s = P.read_text()

def once(old, new, label):
    global s
    c = s.count(old)
    if c != 1:
        sys.exit(f"[{label}] expected 1 match, got {c}")
    s = s.replace(old, new)

# 1. Hoist the fallback pulse out of the top bar (it now drives the chip).
a = s.index("                        // Fallback pulse animation (iOS: 3× red pulse on model switch)")
b_marker = "                        Column(\n                            horizontalAlignment = Alignment.Start,"
b = s.index(b_marker, a)
block = s[a:b]
s = s[:a] + s[b:]
hoisted = "\n".join(l[16:] if l.startswith(" " * 16) else l for l in block.split("\n"))
once("    Scaffold(\n        containerColor = ChatColors.background,",
     "    // [T-spectra-v3-composer] Hoisted from the header: the fallback pulse now\n"
     "    // flashes the composer's model chip, where the model switch lives.\n"
     + hoisted.rstrip() + "\n"
     "    Scaffold(\n        containerColor = ChatColors.background,", "hoist")

# 2. Title column no longer pulses.
once("                                .background(Color.Red.copy(alpha = 0.35f * fallbackPulseAlpha.value))\n", "", "title-pulse")

# 3. Drop the model subtitle row from the header; live status sits under the title.
a = s.index("                            // Model picker subtitle: green dot + group +")
end_marker = "\n                        }\n                    }\n                        SpectraLiveChip(isStreaming = isStreaming, modifier = Modifier.padding(start = 6.dp))\n"
b = s.index(end_marker, a)
s = s[:a] + (
    "                            // [T-spectra-v3-composer] The model chip + thinking\n"
    "                            // badge moved into the composer (SpectraModelChip /\n"
    "                            // SpectraDepthChip). The header is the chat's name\n"
    "                            // and, while a turn runs, its live status.\n"
    "                            SpectraLiveChip(\n"
    "                                isStreaming = isStreaming,\n"
    "                                modifier = Modifier.padding(start = 4.dp, top = 3.dp),\n"
    "                            )"
) + s[b:]
# Right-hand live chip is gone (it now sits under the title).
once(end_marker, "\n                        }\n                    }\n", "live-chip-right")

once("                expandedHeight = 62.dp,", "                expandedHeight = 58.dp,", "height")

# 4. Tray state.
once("    var showThinkingLevelSheet by remember { mutableStateOf(false) }",
     "    var showThinkingLevelSheet by remember { mutableStateOf(false) }\n"
     "    // [T-spectra-v3-composer] Inline depth slider inside the composer card.\n"
     "    var showDepthTray by remember { mutableStateOf(false) }", "tray-state")

# 5. Tray inside the card, above the button row.
once("                    // Button row below text field (iOS layout: + / ... mic send)\n",
     "                    // [T-spectra-v3-composer] Thinking depth slider (inline tray).\n"
     "                    val composerThinkingLevel by viewModel.thinkingLevel.collectAsState()\n"
     "                    val composerDepthEligible = viewModel.availableThinkingLevels.isNotEmpty() &&\n"
     "                        (composerThinkingLevel.isEnabled || viewModel.currentModelSupportsReasoning)\n"
     "                    androidx.compose.animation.AnimatedVisibility(\n"
     "                        visible = showDepthTray && composerDepthEligible,\n"
     "                        enter = androidx.compose.animation.expandVertically(tween(240)) +\n"
     "                            androidx.compose.animation.fadeIn(tween(200)),\n"
     "                        exit = androidx.compose.animation.shrinkVertically(tween(200)) +\n"
     "                            androidx.compose.animation.fadeOut(tween(150)),\n"
     "                    ) {\n"
     "                        SpectraDepthTray(\n"
     "                            current = composerThinkingLevel,\n"
     "                            available = viewModel.availableThinkingLevels,\n"
     "                            onSelect = { viewModel.setThinkingLevel(it) },\n"
     "                        )\n"
     "                    }\n\n"
     "                    // Button row below text field (iOS layout: + / ... mic send)\n", "tray")

# 6. Chips after the slash button.
slash_end = (
    "                                fontStyle = FontStyle.Italic,\n"
    "                                color = MaterialTheme.colorScheme.onSurfaceVariant,\n"
    "                            )\n"
    "                        }\n"
)
chips = (
    "\n"
    "                        // [T-spectra-v3-composer] Model switch + thinking depth.\n"
    "                        Row(\n"
    "                            verticalAlignment = Alignment.CenterVertically,\n"
    "                            modifier = Modifier.weight(1f).padding(start = 8.dp),\n"
    "                        ) {\n"
    "                            val chipFastEligible by viewModel.showFastModeToggle.collectAsState()\n"
    "                            val chipFastOn by viewModel.fastModeEnabled.collectAsState()\n"
    "                            val chipLabel = modelName.ifEmpty { selectedGroupName }.ifEmpty {\n"
    "                                stringResource(R.string.model_picker_no_model_selected)\n"
    "                            }\n"
    "                            SpectraModelChip(\n"
    "                                label = chipLabel,\n"
    "                                groupBound = selectedGroupId != null,\n"
    "                                resolved = modelName.isNotEmpty(),\n"
    "                                fastOn = chipFastEligible && chipFastOn,\n"
    "                                pulse = fallbackPulseAlpha.value,\n"
    "                                interactionSource = modelPickerInteraction,\n"
    "                                onClick = { showModelPicker = true },\n"
    "                                modifier = Modifier.weight(1f, fill = false),\n"
    "                            )\n"
    "                            if (composerDepthEligible) {\n"
    "                                Spacer(modifier = Modifier.width(6.dp))\n"
    "                                SpectraDepthChip(\n"
    "                                    level = composerThinkingLevel,\n"
    "                                    stops = listOf(com.openminis.app.data.model.ThinkingLevel.OFF) +\n"
    "                                        viewModel.availableThinkingLevels,\n"
    "                                    open = showDepthTray,\n"
    "                                    onClick = { showDepthTray = !showDepthTray },\n"
    "                                    onLongClick = { showThinkingLevelSheet = true },\n"
    "                                )\n"
    "                            }\n"
    "                        }\n"
)
i = s.index("viewModel.setInputText(viewModel.showSlashMenuOverInput(inputText))")
j = s.index(slash_end, i) + len(slash_end)
s = s[:j] + chips + s[j:]

# The flexible spacer after the edit pill becomes a fixed gap (the chip row
# above already takes the slack).
k = s.index("                        Spacer(modifier = Modifier.weight(1f))\n\n                        // Right: Mic button", j)
s = s[:k] + "                        Spacer(modifier = Modifier.width(6.dp))\n\n                        // Right: Mic button" + s[k + len("                        Spacer(modifier = Modifier.weight(1f))\n\n                        // Right: Mic button"):]

P.write_text(s)
print("ok")
