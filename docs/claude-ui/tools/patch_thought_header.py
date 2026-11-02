"""[T-claude-ui] Thinking card -> design board r5 "思维链 · 贴近 Claude".

No outer card: collapsed = one quiet row (16dp glyph · label · seconds · chevron),
live label shimmers, expanded steps hang from a 1.5dp rail. Card surface,
edge and shadow become transparent so the welded-card machinery stays intact.
"""
import pathlib, re
root = pathlib.Path(__file__).resolve().parents[3] / 'src/android/app/src/main/java/com/openminis/app/ui/chat'

tm = root / 'ThoughtMotion.kt'
s = tm.read_text()
a = s.index('@Composable\ninternal fun ThoughtHeader(')
b = s.index('@Composable\ninternal fun ThoughtChevron(')
new = '''@Composable
internal fun ThoughtHeader(
    orb: OrbState,
    title: String,
    meta: String,
    liveSecs: Long?,
    preview: String,
    expanded: Boolean,
    failed: String?,
    tools: List<String>,
    toolsKey: String,
    animateTools: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val dark = ChatColors.isDark
    val live = liveSecs != null
    val labelInk = if (dark) Noir.IvorySoft else Noir.InkSoft
    val metaInk = if (dark) Noir.IvoryMute else Noir.InkFaint
    val chevInk = if (dark) Noir.IvoryMute else Noir.InkMute
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .offset(x = (-8).dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            when {
                live -> androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = if (dark) Noir.Ivory else Noir.Ink,
                    trackColor = if (dark) Noir.OnyxHairlineStrong else Noir.PaperSunk2,
                )
                failed != null -> Icon(
                    androidx.compose.material.icons.Icons.Outlined.ErrorOutline, null,
                    tint = Noir.Err, modifier = Modifier.size(16.dp),
                )
                tools.isEmpty() -> Icon(
                    androidx.compose.material.icons.Icons.Outlined.Schedule, null,
                    tint = chevInk, modifier = Modifier.size(16.dp),
                )
                else -> Icon(
                    toolIconFor(tools.last()), null,
                    tint = chevInk, modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        val label = title
        if (live) {
            ShimmerLabel(text = label, base = metaInk, hi = if (dark) Noir.Ivory else Noir.Ink)
        } else {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = labelInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        val metaText = when {
            live -> "${liveSecs} 秒".takeIf { java.util.Locale.getDefault().language == "zh" } ?: "${liveSecs}s"
            failed != null -> failed
            else -> meta
        }
        if (metaText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = metaText,
                fontSize = 13.sp,
                color = if (failed != null && !live) Noir.Err else metaInk,
                maxLines = 1,
                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        if (trailing != null) {
            trailing()
        } else {
            val rot by animateFloatAsState(if (expanded) 90f else 0f, tween(220, easing = ThoughtEase), label = "thoughtChev")
            Icon(
                androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = chevInk,
                modifier = Modifier.size(16.dp).rotate(rot),
            )
        }
    }
}

/** Live label: grey text with an ink band sweeping left → right (design r5). */
@Composable
private fun ShimmerLabel(text: String, base: Color, hi: Color) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "thoughtShimmer")
    val p by t.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(tween(1800, easing = androidx.compose.animation.core.LinearEasing)),
        label = "thoughtShimmerP",
    )
    Text(
        text = text,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = androidx.compose.ui.text.TextStyle(
            brush = object : androidx.compose.ui.graphics.ShaderBrush() {
                override fun createShader(size: Size): androidx.compose.ui.graphics.Shader {
                    val w = size.width.coerceAtLeast(1f)
                    val c = p * w
                    return androidx.compose.ui.graphics.LinearGradientShader(
                        from = Offset(c - w * 0.35f, 0f), to = Offset(c + w * 0.35f, 0f),
                        colors = listOf(base, hi, base),
                    )
                }
            },
        ),
    )
}

/** Stroke glyph per tool family, shared by the header and the step rows. */
internal fun toolIconFor(tool: String?): androidx.compose.ui.graphics.vector.ImageVector {
    val n = tool?.lowercase().orEmpty()
    val I = androidx.compose.material.icons.Icons.Outlined
    return when {
        "search" in n || "web" in n -> I.Search
        "shell" in n || "exec" in n || "terminal" in n || "run" in n || "code" in n -> I.Code
        "read" in n || "file" in n -> I.Description
        "write" in n || "edit" in n -> I.Edit
        "browser" in n -> I.Language
        "memory" in n -> I.Bookmarks
        "subagent" in n || "agent" in n -> I.Hub
        else -> I.Build
    }
}

'''
s = s[:a] + new + s[b:]
s = s.replace(
    "internal fun thoughtShadow(): Color = if (ChatColors.isDark) Color.Transparent else Color(0x1A0A0A0B)",
    "internal fun thoughtShadow(): Color = Color.Transparent")
for imp in ['androidx.compose.material.icons.outlined.ErrorOutline', 'androidx.compose.material.icons.outlined.Schedule',
            'androidx.compose.material.icons.outlined.Search', 'androidx.compose.material.icons.outlined.Code',
            'androidx.compose.material.icons.outlined.Description', 'androidx.compose.material.icons.outlined.Edit',
            'androidx.compose.material.icons.outlined.Language', 'androidx.compose.material.icons.outlined.Bookmarks',
            'androidx.compose.material.icons.outlined.Hub', 'androidx.compose.material.icons.outlined.Build',
            'androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight', 'androidx.compose.ui.unit.sp']:
    line = f'import {imp}\n'
    if line not in s:
        s = s.replace('import androidx.compose.ui.Alignment\n', line + 'import androidx.compose.ui.Alignment\n', 1)
tm.write_text(s)

st = root / 'ChatStepTimeline.kt'
t = st.read_text()
t = t.replace(
    "internal fun thoughtCardFill(): Color = if (ChatColors.isDark) Noir.Onyx1 else Noir.PaperCard",
    "internal fun thoughtCardFill(): Color = Color.Transparent")
t = t.replace(
    "internal fun thoughtCardEdge(): Color = if (ChatColors.isDark) Noir.OnyxHairlineStrong else Noir.PaperHairline",
    "internal fun thoughtCardEdge(): Color = Color.Transparent")
st.write_text(t)
print('thought header patched')
