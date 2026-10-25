#!/usr/bin/env python3
"""[T-spectra-v3-home] Session list → v3 structure: plain section titles with a
count, each date bucket welded into ONE white card with hairline dividers, and
rows reduced to title/time + preview (category glyph shrinks to a tinted mark,
running sessions show the live orb). Folders adopt the same 16dp card inset."""
import pathlib, sys

P = pathlib.Path("src/android/app/src/main/java/com/openminis/app/ui/sessions/SessionListScreen.kt")
s = P.read_text()

def once(old, new, label):
    global s
    c = s.count(old)
    if c != 1:
        sys.exit(f"[{label}] expected 1 match, got {c}")
    s = s.replace(old, new)

# ── 1. Section header with optional count ────────────────────────────────
a = s.index("@Composable\nprivate fun SectionHeader(title: String) {")
b = s.index("// ─── Session Item", a)
s = s[:a] + '''@Composable
private fun SectionHeader(title: String, count: Int? = null) {
    // T172: title may now be a localized string, so compare against the
    // localized "Pinned" rather than the hardcoded enum label.
    val isPinned = title == stringResource(R.string.sessionlist_section_pinned)
    // [T-spectra-v3-home] v3 `.sec`: 17sp title left, quiet count right.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 10.dp),
    ) {
        if (isPinned) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (count != null && count > 0) {
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

''' + s[b:]

# ── 2. renderSessionRows: carded date buckets ────────────────────────────
once('''                            inFolder: Boolean = false,
                        ) {
                            items(rows, key = { it.id }) { session ->''',
'''                            inFolder: Boolean = false,
                            // [T-spectra-v3-home] Date buckets weld their rows
                            // into one white card (v3 `.list`).
                            carded: Boolean = false,
                        ) {
                            items(rows, key = { it.id }) { session ->''', "carded-param")

once('''                                } else {
                                    Modifier
                                }
                                // animateItem gives the accordion its motion:''',
'''                                } else if (carded) {
                                    val isFirst = session.id == rows.first().id
                                    val isLast = session.id == rows.last().id
                                    val seg = when {
                                        isFirst && isLast -> FolderSegment.LONE
                                        isFirst -> FolderSegment.TOP
                                        isLast -> FolderSegment.BOTTOM
                                        else -> FolderSegment.MIDDLE
                                    }
                                    val r = 16.dp
                                    Modifier
                                        .padding(horizontal = 16.dp)
                                        .folderSurface(
                                            segment = seg,
                                            fill = folderFillColor(),
                                            edge = folderEdgeColor(),
                                        )
                                        .then(if (!isFirst) Modifier.spectraRowDivider(spectraDividerColor()) else Modifier)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = if (isFirst) r else 0.dp,
                                                topEnd = if (isFirst) r else 0.dp,
                                                bottomStart = if (isLast) r else 0.dp,
                                                bottomEnd = if (isLast) r else 0.dp,
                                            ),
                                        )
                                } else {
                                    Modifier
                                }
                                // animateItem gives the accordion its motion:''', "carded-mod")

# Folder members: 16dp inset + divider under the folder header / previous row.
once('''                                    Modifier
                                        .padding(
                                            start = 6.dp, end = 6.dp,
                                            bottom = if (isLast) 4.dp else 0.dp,
                                        )
                                        .folderSurface(
                                            segment = if (isLast) FolderSegment.BOTTOM
                                            else FolderSegment.MIDDLE,
                                            fill = folderFillColor(),
                                            edge = folderEdgeColor(),
                                        )''',
'''                                    Modifier
                                        .padding(
                                            start = 16.dp, end = 16.dp,
                                            bottom = if (isLast) 4.dp else 0.dp,
                                        )
                                        .folderSurface(
                                            segment = if (isLast) FolderSegment.BOTTOM
                                            else FolderSegment.MIDDLE,
                                            fill = folderFillColor(),
                                            edge = folderEdgeColor(),
                                        )
                                        .spectraRowDivider(spectraDividerColor())''', "folder-members")

once('''                                    rowBackground = when {
                                        session.id == selectedSessionId ->
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        inFolder -> Color.Transparent
                                        else -> null
                                    },
                                    isFolderMember = inFolder,''',
'''                                    rowBackground = when {
                                        session.id == selectedSessionId ->
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        inFolder || carded -> Color.Transparent
                                        else -> null
                                    },
                                    // Carded rows are welded exactly like folder
                                    // members (inset tint, segment-shaped ripple).
                                    isFolderMember = inFolder || carded,''', "row-bg")

once('''                                SectionHeader(title = stringResource(R.string.sessionlist_section_pinned))
                            }
                            renderSessionRows(periodSessions)''',
'''                                SectionHeader(
                                    title = stringResource(R.string.sessionlist_section_pinned),
                                    count = periodSessions.size,
                                )
                            }
                            renderSessionRows(periodSessions, carded = true)''', "pinned")

once('''                                SectionHeader(title = stringResource(R.string.group_section_header))''',
'''                                SectionHeader(
                                    title = stringResource(R.string.group_section_header),
                                    count = folderBlocks.size,
                                )''', "groups-header")

once('''                                    DatePeriod.EARLIER -> R.string.sessionlist_section_earlier
                                }))
                            }
                            renderSessionRows(periodSessions)''',
'''                                    DatePeriod.EARLIER -> R.string.sessionlist_section_earlier
                                }), count = periodSessions.size)
                            }
                            renderSessionRows(periodSessions, carded = true)''', "trailing")

# Folder card outer inset 6 → 16 to sit on the same card column.
once('''            .padding(
                start = 6.dp, end = 6.dp, top = 4.dp,
                bottom = if (isExpandedWithRows) 0.dp else 4.dp,
            )
            .folderSurface(''',
'''            .padding(
                start = 16.dp, end = 16.dp, top = 4.dp,
                bottom = if (isExpandedWithRows) 0.dp else 4.dp,
            )
            .folderSurface(''', "folder-card")

# Quieter edge (v3 --line).
once('''private fun folderEdgeColor(): Color =
    if (ChatColors.isDark) Color.White.copy(alpha = 0.30f)
    else Color.Black.copy(alpha = 0.08f)''',
'''private fun folderEdgeColor(): Color =
    if (ChatColors.isDark) Color.White.copy(alpha = 0.10f)
    else Color.Black.copy(alpha = 0.07f)

/** [T-spectra-v3-home] Hairline between welded rows (v3 `.it` border). */
@Composable
private fun spectraDividerColor(): Color =
    if (ChatColors.isDark) Color.White.copy(alpha = 0.07f)
    else Color.Black.copy(alpha = 0.06f)

/** Draws a hairline across the TOP of a welded row, inset 16dp each side. */
private fun Modifier.spectraRowDivider(color: Color): Modifier = drawBehind {
    val inset = 16.dp.toPx()
    val y = 0.5.dp.toPx()
    drawLine(
        color = color,
        start = Offset(inset, y),
        end = Offset(size.width - inset, y),
        strokeWidth = 1.dp.toPx(),
    )
}''', "edge")

# ── 3. Row content ───────────────────────────────────────────────────────
a = s.index("        // Category icon in colored circle (18% opacity matching iOS)")
end_txt = '''        // Relative timestamp
        Text(
            text = timeText,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
'''
b = s.index(end_txt, a) + len(end_txt)
s = s[:a] + '''        // [T-sidebar-subagent-running] Running sub agents count as this
        // session working (see the folder aggregate above).
        val activeSessions by SessionActivityTracker.activeSessions.collectAsState()
        val agentWorkParents by AgentJobRegistry.parentsWithAgentWork.collectAsState()
        val isActive = session.id in activeSessions || session.id in agentWorkParents
        // [T-android-session-paused-badge] Head of this session's badge queue.
        val badgeMap by com.openminis.app.service.SessionBadgeStore.byId.collectAsState()
        val badgeHead = badgeMap[session.id]?.firstOrNull()

        // [T-spectra-v3-home] v3 row: no icon disc. Line 1 = category mark
        // (or the live orb while running) + title + time; line 2 = preview,
        // aligned under the title.
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(16.dp),
                ) {
                    if (isActive) {
                        com.openminis.app.ui.theme.SpectraOrb(
                            size = 15.dp,
                            state = com.openminis.app.ui.theme.OrbState.Live,
                        )
                    } else {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = style.color,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                val titleText = session.title ?: "New Chat"
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        highlightedAnnotatedString(titleText, searchQuery)
                    } else {
                        androidx.compose.ui.text.AnnotatedString(titleText)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (badgeHead != null) {
                    SessionBadgeOverlay(
                        state = badgeHead,
                        modifier = Modifier.padding(start = 6.dp, end = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = timeText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            // During an active search, prefer the matched message snippet
            // (content hit) over the generic lastMessage preview.
            if (searchQuery.isNotBlank() && searchSnippet != null) {
                Text(
                    text = highlightedAnnotatedString(searchSnippet, searchQuery),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                )
            } else {
                Text(
                    text = session.lastMessage ?: "No messages yet",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                )
            }
        }
''' + s[b:]

# Welded rows: 16dp inner lead (card inset is 16 too) and v3 15dp rhythm.
once('''                horizontal = 10.dp,
                // [T-android-selection-no-reflow] Back to the original 12dp.''',
'''                horizontal = if (inFolder) 16.dp else 10.dp,
                // [T-android-selection-no-reflow] Back to the original 12dp.''', "row-pad")
once('''                vertical = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),''',
'''                vertical = 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),''', "row-vpad")

P.write_text(s)
print("ok")
