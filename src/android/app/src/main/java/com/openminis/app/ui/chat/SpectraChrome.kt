package com.openminis.app.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.ui.components.decorativePhase
import com.openminis.app.ui.components.decorativePingPong
import com.openminis.app.ui.components.rememberDecorativeTick
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.Noir

/*
 * [T-spectra-v3] Chat chrome from the Spectra v3 board:
 *  - SpectraIsland: the top bar floats as a rounded "status island" over the
 *    page (soft shadow, no border in light mode, hairline in dark).
 *  - SpectraLiveChip: "Running 00:41" — the ONE place a turn's progress is
 *    stated. Green pulse dot, tabular digits.
 *  - SpectraContextRing: context-window usage as a thin ink ring with the
 *    percent inside; taps open the token usage sheet.
 */

@Composable
internal fun SpectraIsland(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = ChatColors.isDark
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ChatColors.background.copy(alpha = 0.94f))
            .statusBarsPadding()
            .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (dark) 0.dp else 14.dp,
                    shape = shape,
                    ambientColor = Color(0x140A0A0B),
                    spotColor = Color(0x290A0A0B),
                )
                .clip(shape)
                .background(if (dark) Noir.Onyx1 else Noir.PaperCard)
                .drawBehind {
                    // Hairline: always in dark, faint in light.
                    val c = if (dark) Noir.OnyxHairline else Noir.PaperHairline
                    drawRoundRect(
                        color = c,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                },
            content = content,
        )
    }
}

@Composable
internal fun SpectraLiveChip(isStreaming: Boolean, modifier: Modifier = Modifier) {
    if (!isStreaming) return
    var startMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        startMs = System.currentTimeMillis()
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    val secs = ((nowMs - startMs) / 1000).coerceAtLeast(0)
    val green = if (ChatColors.isDark) Noir.OkOnOnyx else Noir.Ok
    val tick = rememberDecorativeTick()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .drawBehind {
                    val p = decorativePingPong(decorativePhase(tick.value, 1600, 0))
                    drawCircle(green.copy(alpha = 0.16f + 0.14f * p), radius = size.minDimension * (1.0f + 0.25f * p))
                    drawCircle(green, radius = size.minDimension / 2f)
                },
        )
        Text(
            text = stringResource(R.string.spectra_island_running, "%02d:%02d".format(secs / 60, secs % 60)),
            fontSize = 12.5.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            color = green,
            maxLines = 1,
        )
    }
}

@Composable
internal fun SpectraContextRing(percent: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dark = ChatColors.isDark
    val track = if (dark) Noir.Onyx3 else Noir.PaperSunk2
    val fg = when {
        percent >= 90 -> if (dark) Noir.ErrOnOnyx else Noir.Err
        percent >= 75 -> Noir.Timeout
        else -> if (dark) Noir.Ivory else Noir.Ink
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Canvas(Modifier.size(32.dp)) {
            val sw = 2.5.dp.toPx()
            val inset = sw / 2
            val arcSize = Size(size.width - sw, size.height - sw)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
            drawArc(
                fg, -90f, 360f * (percent.coerceIn(0, 100) / 100f), false,
                Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "$percent",
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ChatColors.secondaryText,
        )
    }
}
