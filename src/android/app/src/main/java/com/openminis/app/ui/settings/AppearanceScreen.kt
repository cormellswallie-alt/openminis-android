package com.openminis.app.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.openminis.app.ui.theme.ExpressiveTokens
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.openminis.app.R
import com.openminis.app.data.repository.AppIconRepository
import com.openminis.app.ui.components.ExpressiveArticulatedSeam
import com.openminis.app.ui.components.MinisTonalButton
import com.openminis.app.ui.theme.ChatColors
import kotlin.math.roundToInt

// -- Preference Keys --
const val PREF_APPEARANCE = "appearance_prefs"
const val KEY_THEME_MODE = "theme_mode"            // 0=System, 1=Light, 2=Dark
const val KEY_DYNAMIC_COLOR = "dynamic_color"      // Boolean, default false (Android 12+ Material You)
const val KEY_LAUNCH_SESSION = "launch_session"    // 0=Auto, 1=LastSession, 2=NewChat, 3=Home
const val KEY_RETURN_KEY_BEHAVIOR = "returnKeyBehavior"  // Int 0=Newline (default), 1=Send
const val KEY_KEEP_SCREEN_AWAKE = "keepScreenAwakeDuringTasks"  // Boolean, default false
const val KEY_TOOL_PREVIEW = "tool_preview"        // Boolean, default true
const val KEY_AUTO_FOCUS_AFTER_REPLY = "chat.autoFocusAfterReply"  // Boolean, default true
const val KEY_SHOW_CHAT_TITLE = "appearance.show_chat_title"  // Boolean, default true
const val KEY_AUTO_EXPAND_THINKING = "chat.autoExpandThinking"  // Boolean, default true
const val KEY_AUTO_GROUPING = "autoGroupingEnabled"  // Boolean, default true
const val KEY_FONT_CHAT_INPUT = "font_chat_input"  // Int scale level -2..3
const val KEY_FONT_MESSAGE = "font_message"        // Int scale level -2..3
const val KEY_FONT_APP_BASE = "font_app_base"      // Int scale level -2..3
const val KEY_LANGUAGE = "app_language"

fun returnKeySendsMessage(context: Context): Boolean =
    getAppearancePrefs(context).getInt(KEY_RETURN_KEY_BEHAVIOR, 0) == 1

fun keepScreenAwakeEnabled(context: Context): Boolean =
    getAppearancePrefs(context).getBoolean(KEY_KEEP_SCREEN_AWAKE, false)

fun autoGroupingEnabled(context: Context): Boolean =
    getAppearancePrefs(context).getBoolean(KEY_AUTO_GROUPING, true)

fun showChatTitleEnabled(context: Context): Boolean =
    getAppearancePrefs(context).getBoolean(KEY_SHOW_CHAT_TITLE, true)

fun autoExpandThinkingEnabled(context: Context): Boolean =
    getAppearancePrefs(context).getBoolean(KEY_AUTO_EXPAND_THINKING, true)

private val APP_ICON_TILE_MAX_WIDTH = 192.dp

private val fontScaleLabels = listOf("XS", "Small", "Default", "Medium", "Large", "XL")
private val fontScaleValues = listOf(-2, -1, 0, 1, 2, 3)
private val fontScaleMultipliers = listOf(0.88f, 0.94f, 1.0f, 1.06f, 1.12f, 1.21f)

private data class LanguageOption(val code: String, val flag: String, val label: String)
private val languageOptions = listOf(
    LanguageOption("", "\uD83C\uDF10", ""),
    LanguageOption("en", "\uD83C\uDDFA\uD83C\uDDF8", "English"),
    LanguageOption("zh", "\uD83C\uDDE8\uD83C\uDDF3", "简体中文"),
    LanguageOption("zh-Hant", "\uD83C\uDDED\uD83C\uDDF0", "\u7E41\u9AD4\u4E2D\u6587"),
    LanguageOption("ja", "\uD83C\uDDEF\uD83C\uDDF5", "日本語"),
    LanguageOption("ko", "\uD83C\uDDF0\uD83C\uDDF7", "한국어"),
    LanguageOption("fr", "\uD83C\uDDEB\uD83C\uDDF7", "Français"),
    LanguageOption("de", "\uD83C\uDDE9\uD83C\uDDEA", "Deutsch"),
    LanguageOption("ru", "\uD83C\uDDF7\uD83C\uDDFA", "\u0420\u0443\u0441\u0441\u043A\u0438\u0439"),
    LanguageOption("es", "\uD83C\uDDEA\uD83C\uDDF8", "Espa\u00F1ol"),
    LanguageOption("id", "\uD83C\uDDEE\uD83C\uDDE9", "Bahasa Indonesia"),
    LanguageOption("ms", "\uD83C\uDDF2\uD83C\uDDFE", "Bahasa Melayu"),
    LanguageOption("fil", "\uD83C\uDDF5\uD83C\uDDED", "Filipino"),
    LanguageOption("th", "\uD83C\uDDF9\uD83C\uDDED", "\u0E44\u0E17\u0E22"),
    LanguageOption("tr", "\uD83C\uDDF9\uD83C\uDDF7", "T\u00FCrk\u00E7e"),
    LanguageOption("pl", "\uD83C\uDDF5\uD83C\uDDF1", "Polski"),
    LanguageOption("ro", "\uD83C\uDDF7\uD83C\uDDF4", "Rom\u00E2n\u0103"),
    LanguageOption("pt-BR", "\uD83C\uDDE7\uD83C\uDDF7", "Portugu\u00EAs (Brasil)"),
    LanguageOption("hr", "\uD83C\uDDED\uD83C\uDDF7", "Hrvatski"),
)

fun getAppearancePrefs(context: Context): SharedPreferences =
    context.getSharedPreferences(PREF_APPEARANCE, Context.MODE_PRIVATE)

fun getThemeMode(context: Context): Int =
    getAppearancePrefs(context).getInt(KEY_THEME_MODE, 0)

fun getFontScale(context: Context, key: String): Float =
    fontScaleForLevel(getAppearancePrefs(context).getInt(key, 0))

fun fontScaleForLevel(level: Int): Float {
    val idx = fontScaleValues.indexOf(level).coerceIn(0, fontScaleMultipliers.lastIndex)
    return fontScaleMultipliers[idx]
}

@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    onThemeChanged: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { getAppearancePrefs(context) }

    var themeMode by remember { mutableIntStateOf(prefs.getInt(KEY_THEME_MODE, 0)) }
    var dynamicColor by remember { mutableStateOf(prefs.getBoolean(KEY_DYNAMIC_COLOR, false)) }
    var launchSession by remember { mutableIntStateOf(prefs.getInt(KEY_LAUNCH_SESSION, 0)) }
    var returnKeyBehavior by remember { mutableIntStateOf(prefs.getInt(KEY_RETURN_KEY_BEHAVIOR, 0)) }
    var keepScreenAwake by remember { mutableStateOf(prefs.getBoolean(KEY_KEEP_SCREEN_AWAKE, false)) }
    var toolPreview by remember { mutableStateOf(prefs.getBoolean(KEY_TOOL_PREVIEW, true)) }
    var autoFocusAfterReply by remember { mutableStateOf(prefs.getBoolean(KEY_AUTO_FOCUS_AFTER_REPLY, true)) }
    var autoExpandThinking by remember { mutableStateOf(prefs.getBoolean(KEY_AUTO_EXPAND_THINKING, true)) }
    var showChatTitle by remember { mutableStateOf(prefs.getBoolean(KEY_SHOW_CHAT_TITLE, true)) }
    var autoGrouping by remember { mutableStateOf(prefs.getBoolean(KEY_AUTO_GROUPING, true)) }
    var chatInputLevel by remember { mutableIntStateOf(prefs.getInt(KEY_FONT_CHAT_INPUT, 0)) }
    var messageLevel by remember { mutableIntStateOf(prefs.getInt(KEY_FONT_MESSAGE, 0)) }
    var appBaseLevel by remember { mutableIntStateOf(prefs.getInt(KEY_FONT_APP_BASE, 0)) }
    var selectedLanguage by remember { mutableStateOf(prefs.getString(KEY_LANGUAGE, "") ?: "") }
    var selectedAppIcon by remember { mutableStateOf(AppIconRepository.current(context)) }

    val fontsModified = chatInputLevel != 0 || messageLevel != 0 || appBaseLevel != 0

    val tilePurple = MaterialTheme.colorScheme.secondary
    val tileBlue = MaterialTheme.colorScheme.primary
    val tileOrange = MaterialTheme.colorScheme.tertiary
    val tileGreen = Color(0xFF1F9D55)
    val tileTeal = Color(0xFF0097A7)

    SettingsScaffold(title = stringResource(R.string.appearance_title), onBack = onBack) {

        // -- Theme & Material You Dynamic Color --
        SettingsSection(
            header = stringResource(R.string.appearance_section_theme),
            footer = stringResource(R.string.appearance_theme_footer),
        ) {
            data class ThemeOption(val label: String, val icon: ImageVector)
            val themeOptions = listOf(
                ThemeOption(stringResource(R.string.appearance_theme_system), Icons.Outlined.BrightnessAuto),
                ThemeOption(stringResource(R.string.appearance_theme_light), Icons.Outlined.LightMode),
                ThemeOption(stringResource(R.string.appearance_theme_dark), Icons.Outlined.DarkMode),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.GroupItemGap),
            ) {
                themeOptions.forEachIndexed { idx, opt ->
                    val checked = themeMode == idx
                    val segShape = when {
                        checked -> RoundedCornerShape(24.dp)
                        idx == 0 -> RoundedCornerShape(
                            topStart = 24.dp,
                            bottomStart = 24.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp,
                        )
                        idx == themeOptions.lastIndex -> RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 8.dp,
                            topEnd = 24.dp,
                            bottomEnd = 24.dp,
                        )
                        else -> RoundedCornerShape(8.dp)
                    }
                    Surface(
                        shape = segShape,
                        color = if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (checked) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(segShape)
                            .clickable {
                                themeMode = idx
                                prefs.edit().putInt(KEY_THEME_MODE, idx).apply()
                                onThemeChanged(idx)
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Filled.Check else opt.icon,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = opt.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ExpressiveArticulatedSeam()
                SettingsSwitchRow(
                    icon = Icons.Outlined.Palette,
                    iconColor = tileOrange,
                    title = "Material You",
                    subtitle = "Android 12+",
                    checked = dynamicColor,
                    onCheckedChange = {
                        dynamicColor = it
                        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, it).apply()
                    },
                    showDivider = false,
                )
            }
        }

        // -- Launch Session (2x2 Expressive Bento Grid) --
        SettingsSection(
            header = stringResource(R.string.appearance_section_launch),
            footer = stringResource(R.string.appearance_launch_footer),
        ) {
            data class LaunchOption(val label: String, val icon: ImageVector, val tint: Color)
            val launchOptions = listOf(
                LaunchOption(stringResource(R.string.appearance_launch_auto), Icons.Outlined.Bolt, tileBlue),
                LaunchOption(stringResource(R.string.appearance_launch_last), Icons.Outlined.History, tileTeal),
                LaunchOption(stringResource(R.string.appearance_launch_new), Icons.Outlined.ChatBubbleOutline, tileGreen),
                LaunchOption(stringResource(R.string.appearance_launch_home), Icons.Outlined.Home, tilePurple),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                launchOptions.chunked(2).forEachIndexed { rowIdx, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pair.forEachIndexed { colIdx, opt ->
                            val idx = rowIdx * 2 + colIdx
                            val selected = launchSession == idx
                            Surface(
                                onClick = {
                                    launchSession = idx
                                    prefs.edit().putInt(KEY_LAUNCH_SESSION, idx).apply()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 66.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    val (badgeBg, badgeFg) = expressiveIconBadgeColors(opt.tint)
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary else badgeBg,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (selected) Icons.Filled.Check else opt.icon,
                                            contentDescription = null,
                                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else badgeFg,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Text(
                                        text = opt.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // -- Return Key (Connected M3 Expressive ButtonGroup) --
        SettingsSection(
            header = stringResource(R.string.appearance_section_return_key),
            footer = stringResource(R.string.appearance_return_key_footer),
        ) {
            data class ReturnOption(val label: String, val value: Int, val icon: ImageVector)
            val returnOptions = listOf(
                ReturnOption(stringResource(R.string.appearance_return_key_newline), 0, Icons.AutoMirrored.Outlined.KeyboardReturn),
                ReturnOption(stringResource(R.string.appearance_return_key_send), 1, Icons.AutoMirrored.Outlined.Send),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.GroupItemGap),
            ) {
                returnOptions.forEachIndexed { idx, opt ->
                    val checked = returnKeyBehavior == opt.value
                    val segShape = when {
                        checked -> RoundedCornerShape(24.dp)
                        idx == 0 -> RoundedCornerShape(
                            topStart = 24.dp,
                            bottomStart = 24.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp,
                        )
                        else -> RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 8.dp,
                            topEnd = 24.dp,
                            bottomEnd = 24.dp,
                        )
                    }
                    Surface(
                        shape = segShape,
                        color = if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (checked) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(segShape)
                            .clickable {
                                returnKeyBehavior = opt.value
                                prefs.edit().putInt(KEY_RETURN_KEY_BEHAVIOR, opt.value).apply()
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Filled.Check else opt.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = opt.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // -- Display & Refresh Rate --
        run {
            val activity = com.openminis.app.ui.RefreshRateController.findActivity(context)
            var refreshMode by remember {
                mutableIntStateOf(com.openminis.app.ui.RefreshRateController.mode(context))
            }
            val supported = remember {
                activity?.let { com.openminis.app.ui.RefreshRateController.supportedRates(it) }.orEmpty()
            }
            val supportedLabel = if (supported.isEmpty()) "?" else supported.joinToString(" / ") { "$it Hz" }
            SettingsSection(
                header = stringResource(R.string.appearance_section_refresh_rate),
                footer = stringResource(R.string.appearance_refresh_rate_footer, supportedLabel),
            ) {
                val rows = listOf(
                    com.openminis.app.ui.RefreshRateController.MODE_HIGHEST to stringResource(R.string.appearance_refresh_rate_highest),
                    com.openminis.app.ui.RefreshRateController.MODE_120 to stringResource(R.string.appearance_refresh_rate_120),
                    com.openminis.app.ui.RefreshRateController.MODE_90 to stringResource(R.string.appearance_refresh_rate_90),
                    com.openminis.app.ui.RefreshRateController.MODE_60 to stringResource(R.string.appearance_refresh_rate_60),
                    com.openminis.app.ui.RefreshRateController.MODE_SYSTEM to stringResource(R.string.appearance_refresh_rate_system),
                )
                rows.forEachIndexed { idx, (mode, label) ->
                    SettingsChoiceRow(
                        title = label,
                        selected = refreshMode == mode,
                        onSelect = {
                            refreshMode = mode
                            com.openminis.app.ui.RefreshRateController.setMode(context, mode)
                            activity?.let { com.openminis.app.ui.RefreshRateController.apply(it) }
                        },
                        leading = if (idx == 0) {
                            {
                                Icon(
                                    Icons.Outlined.Speed,
                                    contentDescription = null,
                                    tint = tileBlue,
                                )
                            }
                        } else {
                            null
                        },
                        showDivider = true,
                    )
                }
                SettingsSwitchRow(
                    icon = Icons.Outlined.ScreenLockPortrait,
                    iconColor = tileGreen,
                    title = stringResource(R.string.appearance_keep_awake_title),
                    subtitle = stringResource(R.string.appearance_keep_awake_footer),
                    checked = keepScreenAwake,
                    onCheckedChange = {
                        keepScreenAwake = it
                        prefs.edit().putBoolean(KEY_KEEP_SCREEN_AWAKE, it).apply()
                    },
                    showDivider = false,
                )
            }
        }

        // -- Consolidated Chat Experience Articulated Stack --
        SettingsSection(
            header = stringResource(R.string.appearance_section_tool_preview),
            footer = stringResource(R.string.appearance_auto_grouping_footer),
        ) {
            SettingsSwitchRow(
                icon = Icons.Outlined.Visibility,
                iconColor = tileTeal,
                title = stringResource(R.string.appearance_tool_preview_title),
                subtitle = stringResource(R.string.appearance_tool_preview_footer),
                checked = toolPreview,
                onCheckedChange = {
                    toolPreview = it
                    prefs.edit().putBoolean(KEY_TOOL_PREVIEW, it).apply()
                },
                showDivider = true,
            )
            SettingsSwitchRow(
                icon = Icons.Outlined.Psychology,
                iconColor = tilePurple,
                title = stringResource(R.string.appearance_auto_expand_thinking_title),
                subtitle = stringResource(R.string.appearance_auto_expand_thinking_footer),
                checked = autoExpandThinking,
                onCheckedChange = {
                    autoExpandThinking = it
                    prefs.edit().putBoolean(KEY_AUTO_EXPAND_THINKING, it).apply()
                },
                showDivider = true,
            )
            SettingsSwitchRow(
                icon = Icons.Outlined.Keyboard,
                iconColor = tileBlue,
                title = stringResource(R.string.appearance_auto_focus_after_reply_title),
                subtitle = stringResource(R.string.appearance_auto_focus_after_reply_footer),
                checked = autoFocusAfterReply,
                onCheckedChange = {
                    autoFocusAfterReply = it
                    prefs.edit().putBoolean(KEY_AUTO_FOCUS_AFTER_REPLY, it).apply()
                },
                showDivider = true,
            )
            SettingsSwitchRow(
                icon = Icons.Outlined.ChatBubbleOutline,
                iconColor = tileBlue,
                title = stringResource(R.string.appearance_show_chat_title),
                subtitle = stringResource(R.string.appearance_show_chat_title_subtitle),
                checked = showChatTitle,
                onCheckedChange = {
                    showChatTitle = it
                    prefs.edit().putBoolean(KEY_SHOW_CHAT_TITLE, it).apply()
                },
                showDivider = true,
            )
            SettingsSwitchRow(
                icon = Icons.Outlined.Folder,
                iconColor = tileOrange,
                title = stringResource(R.string.appearance_auto_grouping),
                subtitle = stringResource(R.string.appearance_auto_grouping_subtitle),
                checked = autoGrouping,
                onCheckedChange = {
                    autoGrouping = it
                    prefs.edit().putBoolean(KEY_AUTO_GROUPING, it).apply()
                },
                showDivider = false,
            )
        }

        // -- Typography & Font Scale Studio --
        SettingsSection(
            header = stringResource(R.string.appearance_section_font_size),
            footer = stringResource(R.string.appearance_font_size_footer),
        ) {
            SettingsRow(
                icon = Icons.Outlined.FormatSize,
                iconColor = tileOrange,
                title = stringResource(R.string.appearance_font_scale_title),
                subtitle = stringResource(R.string.appearance_font_scale_subtitle),
                onClick = null,
                showChevron = false,
                showDivider = false,
            )

            // Live M3 Expressive Typography Preview Stage
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = 20.dp,
                                    bottomEnd = 5.dp,
                                ),
                            )
                            .background(ChatColors.userBubble)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Material 3 Expressive UI",
                            fontSize = (15f * fontScaleForLevel(messageLevel)).sp,
                            fontWeight = FontWeight.Medium,
                            color = ChatColors.onUserBubble,
                        )
                    }
                    Text(
                        text = "Aa Bb Cc · 123 · Expressive Typography",
                        fontSize = (14f * fontScaleForLevel(chatInputLevel)).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ExpressiveArticulatedSeam()

            FontScaleSliderRow(
                label = stringResource(R.string.appearance_font_chat_input),
                level = chatInputLevel,
                onLevelChange = {
                    chatInputLevel = it
                    prefs.edit().putInt(KEY_FONT_CHAT_INPUT, it).apply()
                },
                showDivider = true,
            )
            FontScaleSliderRow(
                label = stringResource(R.string.appearance_font_message),
                level = messageLevel,
                onLevelChange = {
                    messageLevel = it
                    prefs.edit().putInt(KEY_FONT_MESSAGE, it).apply()
                },
                showDivider = true,
            )
            FontScaleSliderRow(
                label = stringResource(R.string.appearance_font_app_base),
                level = appBaseLevel,
                onLevelChange = {
                    appBaseLevel = it
                    prefs.edit().putInt(KEY_FONT_APP_BASE, it).apply()
                },
                showDivider = fontsModified,
            )
            if (fontsModified) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    MinisTonalButton(onClick = {
                        chatInputLevel = 0; messageLevel = 0; appBaseLevel = 0
                        prefs.edit()
                            .putInt(KEY_FONT_CHAT_INPUT, 0)
                            .putInt(KEY_FONT_MESSAGE, 0)
                            .putInt(KEY_FONT_APP_BASE, 0)
                            .apply()
                    }) {
                        Text(
                            stringResource(R.string.appearance_font_reset),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // -- App Icon --
        SettingsSection(
            header = stringResource(R.string.appearance_section_app_icon),
            footer = stringResource(R.string.appearance_app_icon_footer),
        ) {
            data class IconOption(
                val variant: AppIconRepository.Variant,
                val titleRes: Int,
                val mipmapRes: Int,
            )
            val iconOptions = listOf(
                IconOption(
                    AppIconRepository.Variant.Auto,
                    R.string.appearance_app_icon_auto,
                    R.mipmap.ic_launcher,
                ),
                IconOption(
                    AppIconRepository.Variant.ClassicLight,
                    R.string.appearance_app_icon_light,
                    R.mipmap.ic_launcher_classic_light,
                ),
                IconOption(
                    AppIconRepository.Variant.ClassicDark,
                    R.string.appearance_app_icon_dark,
                    R.mipmap.ic_launcher_classic_dark,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    12.dp,
                    Alignment.CenterHorizontally,
                ),
            ) {
                for (option in iconOptions) {
                    val isSelected = selectedAppIcon == option.variant
                    val iconPainter: Painter = remember(option.mipmapRes) {
                        val drawable = ResourcesCompat.getDrawable(
                            context.resources,
                            option.mipmapRes,
                            context.theme,
                        )
                        if (drawable != null) {
                            BitmapPainter(
                                drawable.toBitmap(width = 192, height = 192).asImageBitmap()
                            )
                        } else {
                            BitmapPainter(
                                android.graphics.Bitmap
                                    .createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                                    .asImageBitmap()
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = APP_ICON_TILE_MAX_WIDTH)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable {
                                if (selectedAppIcon != option.variant) {
                                    selectedAppIcon = option.variant
                                    AppIconRepository.apply(context, option.variant)
                                }
                            }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    },
                                    shape = RoundedCornerShape(22.dp),
                                ),
                        ) {
                            Image(
                                painter = iconPainter,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(22.dp)),
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(26.dp)
                                        .background(MaterialTheme.colorScheme.primary, MaterialShapes.Sunny.toShape()),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                        ) {
                            Text(
                                stringResource(option.titleRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }

        // -- Language --
        SettingsSection(
            header = stringResource(R.string.appearance_section_language),
            footer = stringResource(R.string.appearance_language_footer),
        ) {
            languageOptions.forEachIndexed { idx, lang ->
                SettingsChoiceRow(
                    title = if (lang.code.isEmpty()) stringResource(R.string.appearance_theme_system) else lang.label,
                    selected = selectedLanguage == lang.code,
                    onSelect = {
                        selectedLanguage = lang.code
                        prefs.edit().putString(KEY_LANGUAGE, lang.code).apply()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val lm = context.getSystemService(LocaleManager::class.java)
                            lm?.applicationLocales = if (lang.code.isEmpty()) {
                                LocaleList.getEmptyLocaleList()
                            } else {
                                LocaleList.forLanguageTags(lang.code)
                            }
                        } else {
                            (context as? android.app.Activity)?.recreate()
                        }
                    },
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(lang.flag, fontSize = 18.sp)
                        }
                    },
                    showDivider = idx < languageOptions.size - 1,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FontScaleSliderRow(
    label: String,
    level: Int,
    onLevelChange: (Int) -> Unit,
    showDivider: Boolean,
) {
    val idx = fontScaleValues.indexOf(level).coerceIn(0, fontScaleValues.lastIndex)
    var sliderPos by remember(level) { mutableFloatStateOf(idx.toFloat()) }
    val currentLabel = fontScaleLabels.getOrElse(sliderPos.roundToInt()) { "Default" }

    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    currentLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "A",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = sliderPos,
                onValueChange = { sliderPos = it },
                onValueChangeFinished = {
                    val newIdx = sliderPos.roundToInt().coerceIn(0, fontScaleValues.lastIndex)
                    sliderPos = newIdx.toFloat()
                    onLevelChange(fontScaleValues[newIdx])
                },
                valueRange = 0f..5f,
                steps = 4,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            )
            Text(
                "A",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    if (showDivider) {
        ExpressiveArticulatedSeam()
    }
}
