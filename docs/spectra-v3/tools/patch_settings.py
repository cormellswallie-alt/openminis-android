"""[T-spectra-v3] Settings in the v3 language.

- SettingsScaffold: large 30sp title (MediumTopAppBar collapses on scroll is
  avoided — a plain TopAppBar keeps every existing action/navigation slot;
  the title just gets the large style and the bar gets more height).
- SettingsSection (both copies + the settings-landing private one + the
  components/ variant): sentence-case header, shadow-layered white card.
- Settings landing: hero card with the orb at the top.
No row, callback, or preference is touched.
"""
import pathlib, sys

ROOT = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui')

def sub(path, pairs):
    p = ROOT / path
    s = p.read_text()
    for a, b in pairs:
        n = s.count(a)
        if n != 1:
            sys.exit(f'{path}: anchor count {n}: {a[:70]!r}')
        s = s.replace(a, b)
    p.write_text(s)

# ---- settings/SettingsComponents.kt ------------------------------------
sub('settings/SettingsComponents.kt', [
    ("""            val titleSlot: @Composable () -> Unit = {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                )
            }""",
     """            // [T-spectra-v3] Large left title; centered modal titles stay compact.
            val titleSlot: @Composable () -> Unit = {
                Text(
                    title,
                    style = if (centerTitle) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    else com.openminis.app.ui.theme.SpectraSurface.LargeTitleStyle.copy(fontSize = 26.sp, lineHeight = 32.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }"""),
    ("""                TopAppBar(
                    title = titleSlot,
                    navigationIcon = navigationSlot,
                    actions = { actions?.invoke() },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )""",
     """                TopAppBar(
                    title = titleSlot,
                    navigationIcon = navigationSlot,
                    actions = { actions?.invoke() },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                    expandedHeight = 68.dp,
                )"""),
    ("""        if (header != null) {
            // [T-noir-redesign] Eyebrow: Inter SemiBold 11sp, 0.18em tracking.
            Text(
                text = header.uppercaseForDisplay(),
                style = com.openminis.app.ui.theme.NoirEyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,""",
     """        if (header != null) {
            // [T-spectra-v3] Sentence-case 14sp header (no ALL-CAPS eyebrow).
            Text(
                text = header,
                style = com.openminis.app.ui.theme.SpectraSurface.SectionHeaderStyle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),"""),
    ("""        val cardShape = RoundedCornerShape(22.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, cardShape),
            content = content,
        )""",
     """        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .spectraCard(),
            content = content,
        )"""),
    ("""                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(10.dp),
                        ),""",
     """                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(11.dp),
                        ),"""),
    ("""                .padding(horizontal = 14.dp, vertical = 12.dp),
            // #10 keep""",
     """                .padding(horizontal = 16.dp, vertical = 12.dp),
            // #10 keep"""),
])

def add_import(path, imp):
    p = ROOT / path
    s = p.read_text()
    if f'import {imp}\n' not in s:
        i = s.index('\nimport ') + 1
        s = s[:i] + f'import {imp}\n' + s[i:]
    p.write_text(s)

for f in ['settings/SettingsComponents.kt', 'components/SettingsSection.kt', 'settings/SettingsScreen.kt']:
    pass

# ---- components/SettingsSection.kt -------------------------------------
sub('components/SettingsSection.kt', [
    ("""        Text(
            text = title.uppercaseForDisplay(),
            style = com.openminis.app.ui.theme.NoirEyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        )
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(content = content)
        }""",
     """        // [T-spectra-v3] Sentence-case header + shadow-layered card.
        com.openminis.app.ui.theme.SpectraSectionHeader(
            text = title,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().spectraCard(),
            content = content,
        )"""),
])

# ---- settings/SettingsScreen.kt (landing) ------------------------------
sub('settings/SettingsScreen.kt', [
    ("""            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },""",
     """            TopAppBar(
                // [T-spectra-v3] Large left title.
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = com.openminis.app.ui.theme.SpectraSurface.LargeTitleStyle,
                    )
                },
                expandedHeight = 72.dp,"""),
    ("""                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(
                title = stringResource(R.string.settings_section_llm_providers),""",
     """                .verticalScroll(rememberScrollState()),
        ) {
            // [T-spectra-v3] Orb hero — the assistant's face greets the page.
            com.openminis.app.ui.theme.SpectraSettingsHero(
                title = "Minis",
                subtitle = stringResource(R.string.spectra_settings_hint),
                modifier = Modifier.padding(top = 8.dp),
            )
            SettingsSection(
                title = stringResource(R.string.settings_section_llm_providers),"""),
    ("""        Text(
            text = title.uppercaseForDisplay(),
            style = com.openminis.app.ui.theme.NoirEyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
        )

        val shape = RoundedCornerShape(22.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(shape)
                .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        ) {""",
     """        // [T-spectra-v3] Sentence-case header + shadow-layered card.
        com.openminis.app.ui.theme.SpectraSectionHeader(
            text = title,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .spectraCard(),
        ) {"""),
])
for f in ['settings/SettingsComponents.kt', 'components/SettingsSection.kt', 'settings/SettingsScreen.kt']:
    add_import(f, 'com.openminis.app.ui.theme.spectraCard')
print('ok')
