package com.openminis.app.ui.chat

import android.content.Context
import com.openminis.app.R
import com.openminis.app.data.model.ThinkingLevel

/**
 * Localized display name for [ThinkingLevel]. The data-layer
 * [ThinkingLevel.displayName] returns hard-coded English (it predates the
 * UI i18n pass and is also consumed by debug surfaces) — this extension
 * provides the user-facing translation for the slash panel + Thinking
 * level chips, threading through Context to reach strings.xml.
 */
// [T-spectra-v3-composer] Depth names are designed in English (product
// decision) — every surface (composer chip, depth slider, list sheet, slash
// panel) shows the same word. The per-locale strings stay in strings.xml and
// are still referenced below so a future switch back is one line.
@Suppress("UNUSED_PARAMETER")
fun ThinkingLevel.localizedName(context: Context): String = displayName

@Suppress("unused")
internal fun ThinkingLevel.localizedNameRes(): Int =
    when (this) {
        ThinkingLevel.OFF -> R.string.thinking_level_off
        ThinkingLevel.LOW -> R.string.thinking_level_low
        ThinkingLevel.MEDIUM -> R.string.thinking_level_medium
        ThinkingLevel.HIGH -> R.string.thinking_level_high
        ThinkingLevel.XHIGH -> R.string.thinking_level_xhigh
        ThinkingLevel.MAX -> R.string.thinking_level_max
        ThinkingLevel.ULTRA -> R.string.thinking_level_ultra
    }
