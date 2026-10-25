package com.openminis.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * [T-spectra-v3] Shared surfaces for grouped pages (Settings and every
 * sub-screen that uses SettingsSection / SettingsScaffold).
 *
 * v3 rules applied here:
 *  - light: white card on the #F7F7F8 page, layered by ONE soft shadow, no
 *    outline. Dark: lifted onyx card + hairline (shadows vanish on black).
 *  - section headers are sentence case, 14sp Medium, ink-2 — the old ALL
 *    CAPS 11sp eyebrow was the "small grey type" the review called out.
 *  - page titles are a large 30sp left-aligned title, not a 20sp app bar.
 */
object SpectraSurface {
    val CardShape: Shape = RoundedCornerShape(22.dp)

    val SectionHeaderStyle = TextStyle(
        fontFamily = SpectraSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.005.em,
    )

    val LargeTitleStyle = TextStyle(
        fontFamily = SpectraSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    )
}

/** Card treatment: shadow-layered white (light) or lifted onyx + hairline (dark). */
@Composable
fun Modifier.spectraCard(shape: Shape = SpectraSurface.CardShape): Modifier {
    val dark = LocalChatPalette.current.isDark
    return if (dark) {
        this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    } else {
        this
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color(0x14000000),
                spotColor = Color(0x1F0A0A0C),
            )
            .clip(shape)
            .background(Color.White)
    }
}

/** Sentence-case section header used above every grouped card. */
@Composable
fun SpectraSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = SpectraSurface.SectionHeaderStyle,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        modifier = modifier,
    )
}

/**
 * Settings landing hero: the assistant orb + "Minis" + one line of context.
 * Purely decorative; tapping does nothing so it can never shadow a setting.
 */
@Composable
fun SpectraSettingsHero(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .spectraCard()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpectraOrb(size = 52.dp, state = OrbState.Idle)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = SpectraSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                letterSpacing = (-0.01).em,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
