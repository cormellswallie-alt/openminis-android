"""[T-spectra-v3] Home: orb wordmark, greeting hero, plain section titles, legible rows."""
import pathlib
p = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/sessions/SessionListScreen.kt')
s = p.read_text()

def rep(old, new, n=1):
    global s
    assert old in s, old[:90]
    s = s.replace(old, new, n)

# 1) wordmark
rep('''                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .drawBehind { drawNoirPearl(wordmarkDark) },
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.app_name),
                                style = com.openminis.app.ui.theme.NoirDisplay.copy(
                                    fontSize = 28.sp,
                                    lineHeight = 30.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }''', '''                        @Suppress("UNUSED_VARIABLE") val wm = wordmarkDark
                        // [T-spectra-v3] Plain MiSans wordmark; the orb lives in the hero below.
                        Text(
                            stringResource(R.string.app_name),
                            style = com.openminis.app.ui.theme.NoirDisplay.copy(
                                fontSize = 22.sp,
                                lineHeight = 26.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )''')

# 2) hero item at index 0 (only when browsing). Keep folder indices in lockstep.
rep('''    val folderHeaderIndices = remember(leadingDateGroups, folderBlocks) {
        buildMap {
            var idx = 0''', '''    // [T-spectra-v3] Greeting hero occupies list index 0 while browsing.
    val showHomeHero = !isSearchActive && !isSelecting && sessions.isNotEmpty()
    val folderHeaderIndices = remember(leadingDateGroups, folderBlocks, showHomeHero) {
        buildMap {
            var idx = if (showHomeHero) 1 else 0''')
rep('''                        // builder and folderHeaderIndices in lockstep.
''', '''                        // builder and folderHeaderIndices in lockstep.
                        if (showHomeHero) {
                            item(key = "__spectra_hero__", contentType = "spectra_hero") {
                                SpectraHomeHero(
                                    sessionCount = sessions.size,
                                    scheduledCount = scheduledTaskCount,
                                )
                            }
                        }
''')

# 3) section header: sentence case, ink, MiSans SemiBold
rep('''        // [T-noir-redesign] Eyebrow: tracked small caps in mute ink.
        Text(
            text = title.uppercase(),
            style = com.openminis.app.ui.theme.NoirEyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )''', '''        // [T-spectra-v3] Plain section title — no tracked caps.
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )''')
rep('''            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(top = 10.dp),''', '''            .padding(horizontal = 22.dp, vertical = 8.dp)
            .padding(top = 14.dp),''')

# 4) rows: snippet colour stays onSurfaceVariant (now 4.5:1), time uses it too
rep('''        Text(
            text = timeText,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline,
        )''', '''        Text(
            text = timeText,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )''')

# 5) hero composable
s += '''

/**
 * [T-spectra-v3] Home hero: the Spectra orb with a time-of-day greeting,
 * then two quiet counters. Pure layout — no state of its own beyond the clock
 * hour read once per composition.
 */
@Composable
private fun SpectraHomeHero(sessionCount: Int, scheduledCount: Int) {
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 14.dp),
    ) {
        com.openminis.app.ui.theme.SpectraOrb(
            size = 64.dp,
            state = com.openminis.app.ui.theme.OrbState.Idle,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = greeting,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.spectra_home_question),
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpectraHeroChip(stringResource(R.string.spectra_home_sessions, sessionCount))
            if (scheduledCount > 0) {
                SpectraHeroChip(stringResource(R.string.spectra_home_scheduled, scheduledCount))
            }
        }
    }
}

@Composable
private fun SpectraHeroChip(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
'''
p.write_text(s)
print('ok')
