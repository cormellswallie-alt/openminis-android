package com.openminis.app.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Display
import com.openminis.app.logging.AppLogger
import com.openminis.app.ui.settings.getAppearancePrefs
import kotlin.math.abs

/**
 * [refresh-rate] Settings › Appearance › Refresh rate.
 *
 * Android lets an app request a display mode for ITS window via
 * `WindowManager.LayoutParams.preferredDisplayModeId` (API 23+) and a hint via
 * `preferredRefreshRate`. Many ROMs (vivo / OPPO / Xiaomi in particular) default
 * third-party apps to 60 Hz unless the window asks for more, which makes
 * scrolling and streaming text look choppy on 120 Hz panels.
 *
 * Modes:
 *  - [MODE_SYSTEM] (0): leave it to the system (upstream behaviour).
 *  - [MODE_HIGHEST] (1): the highest refresh rate the panel supports at the
 *    current resolution — 120 Hz / 144 Hz on most flagship phones.
 *  - [MODE_120] (2): request 120 Hz specifically (nearest supported mode).
 *  - [MODE_90] (3): 90 Hz.
 *  - [MODE_60] (4): force 60 Hz to save battery.
 *
 * The system may still override the request (battery saver, thermal limits,
 * per-app ROM policies); that is outside an app's control.
 */
object RefreshRateController {
    private const val TAG = "RefreshRate"

    const val KEY_REFRESH_RATE_MODE = "display.refreshRateMode"

    const val MODE_SYSTEM = 0
    const val MODE_HIGHEST = 1
    const val MODE_120 = 2
    const val MODE_90 = 3
    const val MODE_60 = 4

    /** Default: ask for the panel's highest rate — the reason this setting exists. */
    const val DEFAULT_MODE = MODE_HIGHEST

    fun mode(context: Context): Int =
        getAppearancePrefs(context).getInt(KEY_REFRESH_RATE_MODE, DEFAULT_MODE)

    fun setMode(context: Context, mode: Int) {
        getAppearancePrefs(context).edit().putInt(KEY_REFRESH_RATE_MODE, mode).apply()
    }

    /** Distinct refresh rates (Hz, rounded) the current display supports at its current resolution. */
    fun supportedRates(activity: Activity): List<Int> {
        val display = displayOf(activity) ?: return emptyList()
        val current = display.mode
        return display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .map { Math.round(it.refreshRate) }
            .distinct()
            .sorted()
    }

    /** Apply the stored preference to [activity]'s window. Safe to call repeatedly. */
    fun apply(activity: Activity) {
        try {
            val mode = mode(activity)
            val window = activity.window ?: return
            val params = window.attributes
            val display = displayOf(activity)
            if (mode == MODE_SYSTEM || display == null) {
                if (params.preferredDisplayModeId != 0 || params.preferredRefreshRate != 0f) {
                    params.preferredDisplayModeId = 0
                    params.preferredRefreshRate = 0f
                    window.attributes = params
                }
                AppLogger.info(TAG, "mode=system")
                return
            }
            val current = display.mode
            val candidates = display.supportedModes.filter {
                it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight
            }.ifEmpty { display.supportedModes.toList() }
            val target: Display.Mode = when (mode) {
                MODE_HIGHEST -> candidates.maxByOrNull { it.refreshRate }
                MODE_120 -> nearest(candidates, 120f)
                MODE_90 -> nearest(candidates, 90f)
                MODE_60 -> nearest(candidates, 60f)
                else -> null
            } ?: return
            if (params.preferredDisplayModeId != target.modeId || params.preferredRefreshRate != target.refreshRate) {
                params.preferredDisplayModeId = target.modeId
                params.preferredRefreshRate = target.refreshRate
                window.attributes = params
            }
            AppLogger.info(
                TAG,
                "mode=$mode → modeId=${target.modeId} ${target.refreshRate}Hz " +
                    "(${target.physicalWidth}x${target.physicalHeight}); supported=${candidates.map { it.refreshRate }}",
            )
        } catch (t: Throwable) {
            AppLogger.warning(TAG, "apply failed: ${t.message}")
        }
    }

    /** Unwrap a (possibly ContextWrapper-wrapped) Compose context to its Activity. */
    fun findActivity(context: Context): Activity? {
        var c: Context? = context
        while (c is android.content.ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    private fun nearest(modes: List<Display.Mode>, hz: Float): Display.Mode? =
        modes.minByOrNull { abs(it.refreshRate - hz) }

    @Suppress("DEPRECATION")
    private fun displayOf(activity: Activity): Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display
        else activity.windowManager.defaultDisplay
}
