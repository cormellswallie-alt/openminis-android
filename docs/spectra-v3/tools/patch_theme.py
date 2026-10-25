"""[T-spectra-v3] Theme.kt: ink/ivory primary, MiSans typography, Spectra shapes."""
import re, pathlib
p = pathlib.Path('src/android/app/src/main/java/com/openminis/app/ui/theme/Theme.kt')
s = p.read_text()

a = s.index('private val NoirLight = lightColorScheme(')
b = s.index('// App-wide FAB accent.')
schemes = '''private val NoirLight = lightColorScheme(
    primary = Noir.Ink,
    onPrimary = Color.White,
    primaryContainer = Noir.PaperSunk,
    onPrimaryContainer = Noir.Ink,
    inversePrimary = Noir.Ivory,
    secondary = Noir.InkSoft,
    onSecondary = Color.White,
    secondaryContainer = Noir.PaperSunk,
    onSecondaryContainer = Noir.Ink,
    tertiary = Noir.ThinkOnPaper,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1EEFF),
    onTertiaryContainer = Color(0xFF2A1C72),
    background = Noir.Paper,
    onBackground = Noir.Ink,
    surface = Noir.Paper,
    onSurface = Noir.Ink,
    surfaceVariant = Noir.PaperCard,
    onSurfaceVariant = Noir.InkMute,
    surfaceTint = Color.Transparent,
    inverseSurface = Noir.Ink,
    inverseOnSurface = Noir.Ivory,
    error = Noir.Err,
    onError = Color.White,
    errorContainer = Color(0xFFFDECEC),
    onErrorContainer = Color(0xFF6B1316),
    outline = Noir.PaperHairlineStrong,
    outlineVariant = Noir.PaperHairline,
    scrim = Color(0xFF000000),
    surfaceBright = Noir.PaperRaised,
    surfaceDim = Noir.PaperSunk,
    surfaceContainerLowest = Noir.Paper,
    surfaceContainerLow = Noir.PaperCard,
    surfaceContainer = Noir.PaperCard,
    surfaceContainerHigh = Noir.PaperRaised,
    surfaceContainerHighest = Noir.PaperRaised,
)

private val NoirDark = darkColorScheme(
    primary = Noir.Ivory,
    onPrimary = Noir.Onyx,
    primaryContainer = Noir.Onyx3,
    onPrimaryContainer = Noir.Ivory,
    inversePrimary = Noir.Ink,
    secondary = Noir.IvorySoft,
    onSecondary = Noir.Onyx,
    secondaryContainer = Noir.Onyx3,
    onSecondaryContainer = Noir.Ivory,
    tertiary = Noir.ThinkOnOnyx,
    onTertiary = Color(0xFF1B1240),
    tertiaryContainer = Color(0xFF2A2350),
    onTertiaryContainer = Color(0xFFE6DFFF),
    background = Noir.Onyx,
    onBackground = Noir.Ivory,
    surface = Noir.Onyx,
    onSurface = Noir.Ivory,
    surfaceVariant = Noir.Onyx1,
    onSurfaceVariant = Noir.IvoryMute,
    surfaceTint = Color.Transparent,
    inverseSurface = Noir.Ivory,
    inverseOnSurface = Noir.Onyx,
    error = Noir.ErrOnOnyx,
    onError = Noir.Onyx,
    errorContainer = Color(0xFF3A1517),
    onErrorContainer = Color(0xFFFFD6D7),
    outline = Noir.OnyxHairlineStrong,
    outlineVariant = Noir.OnyxHairline,
    scrim = Color(0xFF000000),
    surfaceBright = Noir.Onyx2,
    surfaceDim = Noir.Onyx,
    surfaceContainerLowest = Noir.Onyx,
    surfaceContainerLow = Noir.Onyx1,
    surfaceContainer = Noir.Onyx1,
    surfaceContainerHigh = Noir.Onyx2,
    surfaceContainerHighest = Noir.Onyx2,
)

'''
s = s[:a] + schemes + s[b:]

s = s.replace('''    extraSmall = RoundedCornerShape(14.dp),   // DropdownMenu, Tooltip, OutlinedTextField default
    small = RoundedCornerShape(14.dp),        // Chip, TextField
    medium = RoundedCornerShape(22.dp),       // Card, Snackbar
    large = RoundedCornerShape(26.dp),        // NavigationDrawer
    extraLarge = RoundedCornerShape(32.dp),   // Dialog, BottomSheet''',
'''    extraSmall = RoundedCornerShape(12.dp),   // DropdownMenu, Tooltip, OutlinedTextField default
    small = RoundedCornerShape(14.dp),        // Chip, TextField
    medium = RoundedCornerShape(20.dp),       // Card, Snackbar
    large = RoundedCornerShape(24.dp),        // NavigationDrawer
    extraLarge = RoundedCornerShape(28.dp),   // Dialog, BottomSheet''')

a = s.index('private fun noirTypography(): Typography {')
b = s.index('\n}\n', a) + 3
typo = '''private fun noirTypography(): Typography {
    // [T-spectra-v3] MiSans for every role. Sizes stay on the M3 scale so
    // layouts keep their rhythm; weights are lifted one step for MiSans
    // (its Regular is 330) and large roles get tighter tracking.
    val b = Typography()
    fun TextStyle.ms(w: FontWeight? = null, track: Float? = null) = copy(
        fontFamily = SpectraSans,
        fontWeight = w ?: fontWeight,
        letterSpacing = track?.em ?: letterSpacing,
    )
    return Typography(
        displayLarge = b.displayLarge.ms(FontWeight.Bold, -0.025f),
        displayMedium = b.displayMedium.ms(FontWeight.Bold, -0.025f),
        displaySmall = b.displaySmall.ms(FontWeight.Bold, -0.02f),
        headlineLarge = b.headlineLarge.ms(FontWeight.Bold, -0.02f),
        headlineMedium = b.headlineMedium.ms(FontWeight.SemiBold, -0.015f),
        headlineSmall = b.headlineSmall.ms(FontWeight.SemiBold, -0.01f),
        titleLarge = b.titleLarge.ms(FontWeight.SemiBold, -0.01f),
        titleMedium = b.titleMedium.ms(FontWeight.SemiBold, 0f),
        titleSmall = b.titleSmall.ms(FontWeight.Medium, 0f),
        bodyLarge = b.bodyLarge.ms(FontWeight.Normal, 0f),
        bodyMedium = b.bodyMedium.ms(FontWeight.Normal, 0f),
        bodySmall = b.bodySmall.ms(FontWeight.Normal, 0f).copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = b.labelLarge.ms(FontWeight.Medium, 0f),
        labelMedium = b.labelMedium.ms(FontWeight.Medium, 0f).copy(fontSize = 12.5.sp),
        labelSmall = b.labelSmall.ms(FontWeight.Medium, 0.01f).copy(fontSize = 12.sp),
    )
}
'''
s = s[:a] + typo + s[b:]
s = re.sub(r'// Round 3 \("Iris"\).*?IrisBright on onyx 6\.4:1\.\n', '''// Round 4 ("Spectra v3"): pure white / ink.
//
//   light  page #F7F7F8, card/chat #FFFFFF, sunk #F4F4F6, ink #0A0A0B
//   dark   page #0A0A0B, card #151517, raised #1C1C1F, text #F4F4F5
//
// `primary` is ink (ivory in dark): switches, buttons, progress are
// monochrome. The only hue in the chrome is the assistant orb.
''', s, flags=re.S)
p.write_text(s)
print('ok')
