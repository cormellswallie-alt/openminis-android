package com.openminis.app.ui.settings

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.Noir

// [T-spectra-v5-bubble] The user turn can sit in a bubble again (default) or
// stay the v3 "question as heading". Seven bubble tones.
const val KEY_USER_BUBBLE = "chat.userBubble"            // Boolean, default true
const val KEY_USER_BUBBLE_TONE = "chat.userBubbleTone"   // String (UserBubbleTone.key), default "mist"

/** Bubble fill + text, per light/dark. Saturated tones keep white text ≥ 4.5:1. */
enum class UserBubbleTone(
    val key: String,
    @StringRes val label: Int,
    val light: Color,
    val lightText: Color,
    val dark: Color,
    val darkText: Color,
) {
    MIST("mist", R.string.user_bubble_tone_mist, Color(0xFFF1F1F3), Noir.Ink, Color(0xFF26262A), Noir.Ivory),
    INK("ink", R.string.user_bubble_tone_ink, Color(0xFF0A0A0B), Color.White, Color(0xFFF4F4F5), Color(0xFF0A0A0B)),
    IRIS("iris", R.string.user_bubble_tone_iris, Color(0xFF6650E8), Color.White, Color(0xFF7A66F2), Color.White),
    OCEAN("ocean", R.string.user_bubble_tone_ocean, Color(0xFF2563EB), Color.White, Color(0xFF3B74F0), Color.White),
    JADE("jade", R.string.user_bubble_tone_jade, Color(0xFF0B8A6A), Color.White, Color(0xFF14A07C), Color.White),
    ROSE("rose", R.string.user_bubble_tone_rose, Color(0xFFD63A6E), Color.White, Color(0xFFE04F80), Color.White),
    CORAL("coral", R.string.user_bubble_tone_coral, Color(0xFFDD4F3B), Color.White, Color(0xFFE8604B), Color.White);

    fun fill(dark: Boolean) = if (dark) this.dark else light
    fun text(dark: Boolean) = if (dark) darkText else lightText

    companion object {
        fun of(key: String?): UserBubbleTone = entries.firstOrNull { it.key == key } ?: MIST
    }
}

/** A string appearance pref as Compose state that follows later edits. */
@Composable
fun rememberAppearanceString(key: String, default: String): State<String> {
    val context = LocalContext.current
    val prefs = remember { getAppearancePrefs(context.applicationContext) }
    val state = remember(key) { mutableStateOf(prefs.getString(key, default) ?: default) }
    DisposableEffect(prefs, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, k ->
            if (k == key) state.value = p.getString(key, default) ?: default
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        state.value = prefs.getString(key, default) ?: default
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

/**
 * Settings body: a live preview bubble, then the seven swatches. The selected
 * swatch grows a ring and a check; the preview cross-fades to the new tone.
 */
@Composable
fun UserBubbleTonePicker(
    selected: UserBubbleTone,
    enabled: Boolean,
    onSelect: (UserBubbleTone) -> Unit,
) {
    val dark = ChatColors.isDark
    val haptics = LocalHapticFeedback.current
    val alpha by animateFloatAsState(if (enabled) 1f else 0.38f, label = "bubbleToneAlpha")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // Preview
        val fill by androidx.compose.animation.animateColorAsState(selected.fill(dark), label = "bubbleFill")
        val ink by androidx.compose.animation.animateColorAsState(selected.text(dark), label = "bubbleInk")
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.user_bubble_preview),
                color = ink,
                fontSize = 15.5.sp,
                lineHeight = 22.sp,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 6.dp, bottomStart = 20.dp))
                    .background(fill)
                    .padding(horizontal = 15.dp, vertical = 9.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBubbleTone.entries.forEach { tone ->
                val on = tone == selected
                val ring by animateDpAsState(
                    if (on) 2.dp else 0.dp,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                    label = "toneRing",
                )
                val scale by animateFloatAsState(
                    if (on) 1f else 0.86f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
                    label = "toneScale",
                )
                val edge = if (dark) Noir.OnyxHairlineStrong else Noir.PaperHairlineStrong
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .then(
                            if (ring > 0.dp) Modifier.border(ring, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier,
                        )
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!on) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(tone)
                        },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(CircleShape)
                            .background(tone.fill(dark))
                            .border(1.dp, edge, CircleShape),
                    ) {
                        if (on) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(tone.label),
                                tint = tone.text(dark),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(selected.label),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
