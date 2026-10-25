"""[T-spectra-v4-thought] Wire thinking cards into ChatScreen, remove the
composer "/" button (typing "/" still opens the menu; the attach menu gains a
"Commands" item) and add the two Appearance switches."""
import pathlib
root = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui')

def once(s, old, new, tag):
    n = s.count(old)
    assert n == 1, f'{tag}: {n} matches'
    return s.replace(old, new)

# ── ChatScreen ───────────────────────────────────────────────────────────
p = root / 'chat/ChatScreen.kt'
s = p.read_text()
s = once(s,
 "                val stepThinkingLevel = viewModel.thinkingLevel.value\n"
 "                val stepRuns = remember(flatItems, isStreaming, stepThinkingLevel) {\n"
 "                    computeStepRuns(flatItems, isStreaming) { row ->",
 "                val stepThinkingLevel = viewModel.thinkingLevel.value\n"
 "                // [T-spectra-v4-thought] Appearance → wrap tool calls in thinking cards.\n"
 "                val thoughtCards by com.openminis.app.ui.settings.rememberAppearanceFlag(\n"
 "                    com.openminis.app.ui.settings.KEY_THOUGHT_CARDS, true,\n"
 "                )\n"
 "                val thoughtAutoExpand = remember { com.openminis.app.ui.settings.autoExpandThinkingEnabled(context) }\n"
 "                val stepRuns = remember(flatItems, isStreaming, stepThinkingLevel, thoughtCards) {\n"
 "                    computeStepRuns(\n"
 "                        flatItems, isStreaming,\n"
 "                        cardMode = thoughtCards,\n"
 "                        autoExpandLive = thoughtAutoExpand,\n"
 "                    ) { row ->",
 'computeStepRuns')
s = once(s,
 "                                .padding(top = if (stepFolded) 0.dp else StepItemGap)",
 "                                // [T-spectra-v4-thought] card segments weld edge to edge.\n"
 "                                .padding(top = if (stepFolded || stepInfo.isCardMember) 0.dp else StepItemGap)",
 'gap')
s = once(s,
 "                                            isLast = item.isLast,\n"
 "                                            railUp = railUp,\n"
 "                                            railDown = railDown,\n"
 "                                        )",
 "                                            isLast = item.isLast,\n"
 "                                            railUp = railUp,\n"
 "                                            railDown = railDown,\n"
 "                                            embedded = stepInfo?.card == true,\n"
 "                                        )",
 'embedded')

# Remove the "/" button.
a = s.index('                        // Left: "/" slash command button (iOS: italic /, bold)')
b = s.index('                        // [T-spectra-v3-composer] Model switch + thinking depth.')
s = s[:a] + s[b:]
# The model chip row used padding(start = 8.dp) after the "/" — now after "+".
# Keep it. The Spacer(8dp) between "+" and "/" was before the slash; drop it.
s = once(s,
 "                        }\n\n                        Spacer(modifier = Modifier.width(8.dp))\n\n"
 "                        // [T-spectra-v3-composer] Model switch + thinking depth.",
 "                        }\n\n"
 "                        // [T-spectra-v3-composer] Model switch + thinking depth.",
 'slash-spacer')
# Commands entry in the "+" menu so the slash panel stays one tap away.
s = once(s,
 "                                DropdownMenuItem(\n"
 "                                    text = { Text(stringResource(R.string.chat_attach_add_file)) },",
 "                                // [T-spectra-v4-thought] \"/\" button removed from the\n"
 "                                // composer row; the command panel lives here (and on\n"
 "                                // typing \"/\").\n"
 "                                DropdownMenuItem(\n"
 "                                    text = { Text(stringResource(R.string.chat_attach_slash_commands)) },\n"
 "                                    leadingIcon = {\n"
 "                                        Text(\n"
 "                                            \"/\",\n"
 "                                            fontSize = 18.sp,\n"
 "                                            fontWeight = FontWeight.Bold,\n"
 "                                            fontStyle = FontStyle.Italic,\n"
 "                                            modifier = Modifier.width(24.dp),\n"
 "                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,\n"
 "                                        )\n"
 "                                    },\n"
 "                                    onClick = {\n"
 "                                        showAttachMenu = false\n"
 "                                        if (!viewModel.showSlashMenu.value) {\n"
 "                                            viewModel.setInputText(viewModel.showSlashMenuOverInput(inputText))\n"
 "                                        }\n"
 "                                    },\n"
 "                                )\n"
 "                                DropdownMenuItem(\n"
 "                                    text = { Text(stringResource(R.string.chat_attach_add_file)) },",
 'commands-item')
p.write_text(s)

# ── Appearance ───────────────────────────────────────────────────────────
p = root / 'settings/AppearanceScreen.kt'
s = p.read_text()
s = once(s,
 "    var autoExpandThinking by remember { mutableStateOf(prefs.getBoolean(KEY_AUTO_EXPAND_THINKING, true)) }\n",
 "    var autoExpandThinking by remember { mutableStateOf(prefs.getBoolean(KEY_AUTO_EXPAND_THINKING, true)) }\n"
 "    var thoughtCards by remember { mutableStateOf(prefs.getBoolean(KEY_THOUGHT_CARDS, true)) }\n"
 "    var thinkingAutoTranslate by remember { mutableStateOf(prefs.getBoolean(KEY_THINKING_AUTO_TRANSLATE, false)) }\n",
 'appearance-state')
s = once(s,
 "            footer = stringResource(R.string.appearance_auto_expand_thinking_footer),\n"
 "        ) {\n"
 "            SettingsSwitchRow(\n"
 "                icon = Icons.Outlined.Psychology,\n"
 "                iconColor = tilePurple,\n"
 "                title = stringResource(R.string.appearance_auto_expand_thinking_title),\n"
 "                checked = autoExpandThinking,\n"
 "                onCheckedChange = {\n"
 "                    autoExpandThinking = it\n"
 "                    prefs.edit().putBoolean(KEY_AUTO_EXPAND_THINKING, it).apply()\n"
 "                },\n"
 "                showDivider = false,\n"
 "            )\n"
 "        }\n",
 "            footer = stringResource(R.string.appearance_auto_expand_thinking_footer),\n"
 "        ) {\n"
 "            SettingsSwitchRow(\n"
 "                icon = Icons.Outlined.Psychology,\n"
 "                iconColor = tilePurple,\n"
 "                title = stringResource(R.string.appearance_auto_expand_thinking_title),\n"
 "                checked = autoExpandThinking,\n"
 "                onCheckedChange = {\n"
 "                    autoExpandThinking = it\n"
 "                    prefs.edit().putBoolean(KEY_AUTO_EXPAND_THINKING, it).apply()\n"
 "                },\n"
 "                showDivider = false,\n"
 "            )\n"
 "        }\n\n"
 "        // [T-spectra-v4-thought] -- Thinking Cards --\n"
 "        SettingsSection(\n"
 "            header = stringResource(R.string.spectra_thinking_done),\n"
 "            footer = stringResource(R.string.appearance_thought_cards_footer),\n"
 "        ) {\n"
 "            SettingsSwitchRow(\n"
 "                icon = Icons.Outlined.Psychology,\n"
 "                iconColor = tilePurple,\n"
 "                title = stringResource(R.string.appearance_thought_cards_title),\n"
 "                checked = thoughtCards,\n"
 "                onCheckedChange = {\n"
 "                    thoughtCards = it\n"
 "                    prefs.edit().putBoolean(KEY_THOUGHT_CARDS, it).apply()\n"
 "                },\n"
 "            )\n"
 "            SettingsSwitchRow(\n"
 "                icon = Icons.Outlined.Psychology,\n"
 "                iconColor = tilePurple,\n"
 "                title = stringResource(R.string.appearance_thinking_translate_title),\n"
 "                subtitle = stringResource(R.string.appearance_thinking_translate_footer),\n"
 "                checked = thinkingAutoTranslate,\n"
 "                onCheckedChange = {\n"
 "                    thinkingAutoTranslate = it\n"
 "                    prefs.edit().putBoolean(KEY_THINKING_AUTO_TRANSLATE, it).apply()\n"
 "                },\n"
 "                showDivider = false,\n"
 "            )\n"
 "        }\n",
 'appearance-section')
p.write_text(s)
print('ok')
