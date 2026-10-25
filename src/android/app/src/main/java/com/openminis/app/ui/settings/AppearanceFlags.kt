package com.openminis.app.ui.settings

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// [T-spectra-v4-thought] Tool calls, thinking and the other steps of one run
// share a single thinking card (interim summaries close the card). Default ON.
const val KEY_THOUGHT_CARDS = "chat.thoughtCards"  // Boolean, default true

// [T-spectra-v4-thought] Auto-translate finished English thinking to Chinese
// on device (ML Kit). Default OFF — the first use downloads a language pack.
const val KEY_THINKING_AUTO_TRANSLATE = "chat.thinkingAutoTranslate"  // Boolean, default false

/**
 * An appearance boolean as Compose state that follows later edits (the chat
 * screen stays composed in the back stack while the user flips the switch in
 * Settings, so a one-shot read would go stale).
 */
@Composable
fun rememberAppearanceFlag(key: String, default: Boolean): State<Boolean> {
    val context = LocalContext.current
    val prefs = remember { getAppearancePrefs(context.applicationContext) }
    val state = remember(key) { mutableStateOf(prefs.getBoolean(key, default)) }
    DisposableEffect(prefs, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, k ->
            if (k == key) state.value = p.getBoolean(key, default)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        state.value = prefs.getBoolean(key, default)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}
