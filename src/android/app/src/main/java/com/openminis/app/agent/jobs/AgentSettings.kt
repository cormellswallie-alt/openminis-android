package com.openminis.app.agent.jobs

import android.content.Context

/**
 * [T-p2-agent-settings] Settings › Agents. One switch, on by default — the
 * zero-configuration experience is the product (design v4 §9); the page
 * exists for the user who wants to turn delegation OFF, not to configure it.
 * Mirrors iOS HelperSettingsView / AIChatViewModel.helpersEnabled: the same
 * preference key, the same default.
 */
object AgentSettings {
    const val PREFS = "agent_settings"
    const val KEY_ENABLED = "agent.helpers.enabled"

    /**
     * [subagent-unlimited] User-configurable cap on concurrently running sub
     * agents (and on how many one assistant turn may start before the rest are
     * queued). Upstream hard-coded 3. [UNLIMITED] is stored as 0.
     */
    const val KEY_MAX_CONCURRENT = "agent.helpers.max_concurrent"
    const val UNLIMITED = 0

    /** Effective ceiling used when the user picks "unlimited" — a sanity bound only. */
    const val UNLIMITED_EFFECTIVE = 64

    /** Choices offered in Settings › Sub Agents. 0 = unlimited. */
    val MAX_CONCURRENT_CHOICES = listOf(3, 5, 8, 12, 20, UNLIMITED)

    /** Default for new installs: no artificial limit. */
    const val DEFAULT_MAX_CONCURRENT = UNLIMITED

    /**
     * Cached so hot paths without a Context (AgentJobRegistry.canStartChildJob,
     * the tool schema) can read it. Loaded by [init] at app start and updated by
     * [setMaxConcurrent].
     */
    @Volatile
    private var cachedRaw: Int = DEFAULT_MAX_CONCURRENT

    fun init(context: Context) {
        cachedRaw = prefs(context).getInt(KEY_MAX_CONCURRENT, DEFAULT_MAX_CONCURRENT)
    }

    /** Raw stored value (0 = unlimited) — for the settings UI. */
    fun maxConcurrentRaw(context: Context): Int =
        prefs(context).getInt(KEY_MAX_CONCURRENT, DEFAULT_MAX_CONCURRENT).also { cachedRaw = it }

    fun setMaxConcurrent(context: Context, value: Int) {
        val v = if (value <= 0) UNLIMITED else value
        prefs(context).edit().putInt(KEY_MAX_CONCURRENT, v).apply()
        cachedRaw = v
    }

    /** True when the user chose "unlimited". */
    val isUnlimited: Boolean get() = cachedRaw <= 0

    /** The number actually enforced. */
    val maxConcurrent: Int
        get() = if (cachedRaw <= 0) UNLIMITED_EFFECTIVE else cachedRaw

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * [T-tools-granular-switches] Delegates to
     * [com.openminis.app.tools.AgentToolSwitch.AGENTS], which is now the single
     * source of truth for "may the assistant delegate".
     *
     * Settings > Agents and Settings > Tools both present this choice, and two
     * independent keys would let them disagree: a user turning delegation off
     * on one page would still see it on, and still get the tool, from the
     * other. [KEY_ENABLED] is kept only so the one-time migration can read a
     * pre-split value.
     */
    fun isEnabled(context: Context): Boolean =
        com.openminis.app.tools.AgentToolSwitch.AGENTS.isEnabled(context)

    fun setEnabled(context: Context, enabled: Boolean) {
        com.openminis.app.tools.AgentToolSwitch.AGENTS.setEnabled(context, enabled)
    }
}
