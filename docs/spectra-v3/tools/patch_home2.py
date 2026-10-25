"""[T-spectra-v3] Home round 2: left-aligned hero + "in progress" carousel.

The hero stays ONE LazyColumn item (folderHeaderIndices already offsets by 1),
the carousel lives inside it, so no index math changes.
"""
import sys
P = 'src/android/app/src/main/java/com/openminis/app/ui/sessions/SessionListScreen.kt'
s = open(P, encoding='utf-8').read()

def once(old, new):
    global s
    n = s.count(old)
    if n != 1:
        sys.exit(f'anchor count {n}: {old[:80]!r}')
    s = s.replace(old, new)

once("""                                SpectraHomeHero(
                                    sessionCount = sessions.size,
                                    scheduledCount = scheduledTaskCount,
                                )""",
"""                                SpectraHomeHero(
                                    sessionCount = sessions.size,
                                    scheduledCount = scheduledTaskCount,
                                    liveSessions = sessions.filter { it.id in activeSessionIds }.take(8)
                                        .map { it.id to (it.title ?: "New Chat") to (it.lastMessage ?: "") },
                                    onOpen = onSessionClickGuarded,
                                )""")

a = s.index("@Composable\nprivate fun SpectraHomeHero(sessionCount: Int, scheduledCount: Int) {")
b = s.index("@Composable\nprivate fun SpectraHeroChip(text: String) {")
s = s[:a] + '''@Composable
private fun SpectraHomeHero(
    sessionCount: Int,
    scheduledCount: Int,
    liveSessions: List<Pair<Pair<String, String>, String>> = emptyList(),
    onOpen: (String) -> Unit = {},
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = stringResource(
        when (hour) {
            in 5..10 -> R.string.spectra_greet_morning
            in 11..13 -> R.string.spectra_greet_noon
            in 14..17 -> R.string.spectra_greet_afternoon
            else -> R.string.spectra_greet_evening
        }
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
    ) {
        // [T-spectra-v3] Left-aligned hero: orb, greeting, one big question.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 22.dp),
        ) {
            com.openminis.app.ui.theme.SpectraOrb(
                size = 44.dp,
                state = if (liveSessions.isNotEmpty()) com.openminis.app.ui.theme.OrbState.Live
                else com.openminis.app.ui.theme.OrbState.Idle,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = greeting,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.spectra_home_question),
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.02).em,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 22.dp),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 22.dp),
        ) {
            SpectraHeroChip(stringResource(R.string.spectra_home_sessions, sessionCount))
            if (scheduledCount > 0) {
                SpectraHeroChip(stringResource(R.string.spectra_home_scheduled, scheduledCount))
            }
        }
        if (liveSessions.isNotEmpty()) {
            Text(
                text = stringResource(R.string.spectra_home_live),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, top = 24.dp, bottom = 10.dp),
            )
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(liveSessions.size, key = { liveSessions[it].first.first }) { i ->
                    val (idTitle, preview) = liveSessions[i]
                    SpectraLiveCard(
                        title = idTitle.second,
                        preview = preview,
                        onClick = { onOpen(idTitle.first) },
                    )
                }
            }
        }
    }
}

/** [T-spectra-v3] A conversation that is working right now (streaming or sub agents). */
@Composable
private fun SpectraLiveCard(title: String, preview: String, onClick: () -> Unit) {
    val dark = com.openminis.app.ui.theme.ChatColors.isDark
    Column(
        modifier = Modifier
            .width(232.dp)
            .shadow(
                elevation = if (dark) 0.dp else 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x1F000000),
            )
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (dark) Modifier.border(0.75.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.openminis.app.ui.theme.SpectraOrb(
                size = 22.dp,
                state = com.openminis.app.ui.theme.OrbState.Live,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.spectra_step_running),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (dark) com.openminis.app.ui.theme.Noir.OkOnOnyx else com.openminis.app.ui.theme.Noir.Ok,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (preview.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = preview,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

''' + s[b:]
open(P, 'w', encoding='utf-8').write(s)
print('ok')
