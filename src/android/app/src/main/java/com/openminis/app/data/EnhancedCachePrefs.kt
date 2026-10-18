package com.openminis.app.data

import android.content.Context
import android.content.SharedPreferences
import com.openminis.app.provider.anthropic.AnthropicProvider.CacheTtlMode

/**
 * [T-android-enhanced-cache] / [T-anthropic-cache-mode] Anthropic prompt-cache
 * preferences.
 *
 *  - [isConfirmed]: the user acknowledged the one-time extra-billing dialog.
 *  - [mode]: the cache TTL strategy, app-wide and persisted (it used to be a
 *    per-chat, in-memory on/off switch). STANDARD = 5 minutes, EXTENDED = every
 *    breakpoint 1 hour, SMART = 1-hour checkpoints + 5-minute moving tail.
 *    Chosen in Settings; the chat-menu "Enhanced Cache" switch toggles between
 *    STANDARD and the last 1-hour strategy picked.
 */
object EnhancedCachePrefs {
    private const val PREFS = "minis_enhanced_cache_prefs"
    private const val KEY_CONFIRMED = "enhancedCacheConfirmed"
    private const val KEY_MODE = "cacheTtlMode"
    private const val KEY_LAST_LONG_MODE = "lastLongCacheTtlMode"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isConfirmed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CONFIRMED, false)

    fun setConfirmed(context: Context) {
        prefs(context).edit().putBoolean(KEY_CONFIRMED, true).apply()
    }

    fun mode(context: Context): CacheTtlMode =
        parse(prefs(context).getString(KEY_MODE, null)) ?: CacheTtlMode.STANDARD

    fun setMode(context: Context, mode: CacheTtlMode) {
        val e = prefs(context).edit().putString(KEY_MODE, mode.name)
        if (mode != CacheTtlMode.STANDARD) e.putString(KEY_LAST_LONG_MODE, mode.name)
        e.apply()
    }

    /** The 1-hour strategy the chat-menu switch turns on (default EXTENDED). */
    fun lastLongMode(context: Context): CacheTtlMode =
        parse(prefs(context).getString(KEY_LAST_LONG_MODE, null))
            ?.takeIf { it != CacheTtlMode.STANDARD } ?: CacheTtlMode.EXTENDED

    private fun parse(v: String?): CacheTtlMode? =
        v?.let { name -> CacheTtlMode.values().firstOrNull { it.name == name } }
}
