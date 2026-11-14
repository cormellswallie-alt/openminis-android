package com.openminis.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.openminis.app.ui.theme.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.openminis.app.ui.theme.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openminis.app.BuildConfig
import com.openminis.app.R
import com.openminis.app.ui.components.ExpressiveArticulatedSeam
import com.openminis.app.ui.components.openExternalUrl
import com.openminis.app.ui.theme.ExpressiveTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onProvidersClick: () -> Unit,
    onModelGroupsClick: () -> Unit,
    onRootfsClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onEnvVarsClick: () -> Unit = {},
    onSkillsClick: () -> Unit = {},
    onTerminalClick: () -> Unit = {},
    onMemoryClick: () -> Unit = {},
    /** [T-p2-agent-settings] Settings › Agents (delegation on/off), below Memory. */
    onAgentsClick: () -> Unit = {},
    onAgentToolsClick: () -> Unit = {},
    // [T-mcp-integration-android] MCP Integrations page, listed directly below
    // Memory. Default no-op for callers that haven't wired the route yet.
    onMcpClick: () -> Unit = {},
    // [T-soul-md] Soul settings page lives between Skills and Memory in the
    // Agent Runtime section; default no-op for callers that haven't wired
    // the route yet.
    onSoulClick: () -> Unit = {},
    onPermissionsClick: () -> Unit = {},
    onUsageClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onLogsClick: () -> Unit = {},
    // T219-2: Mount External Folders entry. Default no-op for any caller
    // that hasn't wired the route yet.
    onMountedFoldersClick: () -> Unit = {},
    // T235: Shared Folders entry (Shared / Skills / Memory). Default no-op
    // for back-compat with callers wired before T235.
    onSharedFoldersClick: () -> Unit = {},
    // T50: Background & Notifications screen (battery optimisation +
    // OEM autostart guidance). Default no-op so older callers/tests
    // don't need to be retrofitted.
    onBackgroundClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var showFeedbackSheet by remember { mutableStateOf(false) }

    val accentBlue = MaterialTheme.colorScheme.primary
    val accentIndigo = MaterialTheme.colorScheme.secondary
    val accentAmber = MaterialTheme.colorScheme.tertiary
    val accentTeal = Color(0xFF0097A7)
    val accentEmerald = Color(0xFF1F9D55)

    SettingsScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
    ) {
        // -- M3 Expressive Control Center Bento Grid --
        ControlCenterBentoGrid(
            onProvidersClick = onProvidersClick,
            onModelGroupsClick = onModelGroupsClick,
            onSoulClick = onSoulClick,
            onRootfsClick = onRootfsClick,
        )

        // -- LLM Providers --
        SettingsSection(
            header = stringResource(R.string.settings_section_llm_providers),
            footer = stringResource(R.string.settings_section_llm_providers_footer),
        ) {
            SettingsRow(
                icon = Icons.Outlined.Lock,
                iconColor = accentBlue,
                title = stringResource(R.string.settings_manage_providers),
                subtitle = stringResource(R.string.settings_manage_providers_subtitle),
                onClick = onProvidersClick,
            )
            SettingsRow(
                icon = Icons.Outlined.Settings,
                iconColor = accentIndigo,
                title = stringResource(R.string.settings_model_groups),
                subtitle = stringResource(R.string.settings_model_groups_subtitle),
                onClick = onModelGroupsClick,
            )
            SettingsRow(
                icon = Icons.Outlined.BarChart,
                iconColor = accentTeal,
                title = stringResource(R.string.settings_token_usage),
                subtitle = stringResource(R.string.settings_token_usage_subtitle),
                onClick = onUsageClick,
                showDivider = false,
            )
        }

        // -- Appearance --
        SettingsSection(header = stringResource(R.string.settings_section_appearance)) {
            SettingsRow(
                icon = Icons.Outlined.Palette,
                iconColor = accentAmber,
                title = stringResource(R.string.settings_section_appearance),
                subtitle = stringResource(R.string.settings_appearance_subtitle),
                onClick = onAppearanceClick,
                showDivider = false,
            )
        }

        // -- Agent Runtime --
        // [T-android-settings-runtime-order] Preserves capability -> identity -> connectivity order:
        // Agent Tools, Sub Agents, Skills, Soul, Memory, MCP, Environment Variables.
        SettingsSection(header = stringResource(R.string.settings_section_agent_runtime)) {
            SettingsRow(
                icon = Icons.Outlined.Handyman,
                iconColor = accentIndigo,
                title = stringResource(R.string.settings_agent_tools),
                subtitle = stringResource(R.string.settings_agent_tools_subtitle),
                onClick = onAgentToolsClick,
            )
            SettingsRow(
                icon = Icons.Outlined.Groups,
                iconColor = com.openminis.app.ui.chat.HelperAccentStatic,
                title = stringResource(R.string.settings_agents),
                subtitle = stringResource(R.string.settings_agents_subtitle),
                onClick = onAgentsClick,
            )
            SettingsRow(
                icon = Icons.Outlined.Extension,
                iconColor = accentBlue,
                title = stringResource(R.string.settings_skills),
                subtitle = stringResource(R.string.settings_skills_subtitle),
                onClick = onSkillsClick,
            )
            SettingsRow(
                icon = Icons.Outlined.AutoAwesome,
                iconColor = accentAmber,
                title = stringResource(R.string.settings_soul),
                subtitle = stringResource(R.string.settings_soul_subtitle),
                onClick = onSoulClick,
            )
            SettingsRow(
                icon = Icons.Outlined.Psychology,
                iconColor = accentIndigo,
                title = stringResource(R.string.settings_memory),
                subtitle = stringResource(R.string.settings_memory_subtitle),
                onClick = onMemoryClick,
            )
            SettingsRow(
                icon = Icons.Outlined.Dashboard,
                iconColor = accentTeal,
                title = stringResource(R.string.settings_mcp),
                subtitle = stringResource(R.string.settings_mcp_subtitle),
                onClick = onMcpClick,
            )
            SettingsRow(
                icon = Icons.Outlined.Terminal,
                iconColor = accentEmerald,
                title = stringResource(R.string.settings_env_vars),
                subtitle = stringResource(R.string.settings_env_vars_subtitle),
                onClick = onEnvVarsClick,
                showDivider = false,
            )
        }

        // -- Storage --
        SettingsSection(header = stringResource(R.string.settings_section_storage)) {
            SettingsRow(
                icon = Icons.Outlined.Inventory2,
                iconColor = accentBlue,
                title = stringResource(R.string.settings_section_storage),
                subtitle = stringResource(R.string.settings_storage_subtitle),
                onClick = onRootfsClick,
            )
            SettingsRow(
                icon = Icons.Outlined.Folder,
                iconColor = accentEmerald,
                title = stringResource(R.string.settings_shared_folders),
                subtitle = stringResource(R.string.settings_shared_folders_subtitle),
                onClick = onSharedFoldersClick,
            )
            SettingsRow(
                icon = Icons.Outlined.FolderShared,
                iconColor = accentAmber,
                title = stringResource(R.string.settings_mount_external_folders),
                subtitle = stringResource(R.string.settings_mount_external_folders_subtitle),
                onClick = onMountedFoldersClick,
            )
            SettingsRow(
                icon = Icons.Outlined.Backup,
                iconColor = accentTeal,
                title = stringResource(R.string.settings_backup_restore),
                subtitle = stringResource(R.string.settings_backup_restore_subtitle),
                onClick = onBackupClick,
                showDivider = false,
            )
        }

        // -- Permissions --
        SettingsSection(header = stringResource(R.string.settings_section_permissions)) {
            SettingsRow(
                icon = Icons.Outlined.Shield,
                iconColor = accentBlue,
                title = stringResource(R.string.settings_section_permissions),
                subtitle = stringResource(R.string.settings_permissions_subtitle),
                onClick = onPermissionsClick,
                showDivider = false,
            )
        }

        // -- Background & Notifications (T50) --
        SettingsSection(
            header = stringResource(R.string.bg_section_header),
            footer = stringResource(R.string.bg_section_footer),
        ) {
            SettingsRow(
                icon = Icons.Outlined.BatteryFull,
                iconColor = accentAmber,
                title = stringResource(R.string.bg_section_header),
                subtitle = stringResource(R.string.bg_section_subtitle),
                onClick = onBackgroundClick,
                showDivider = false,
            )
        }

        // -- Logs --
        SettingsSection(header = stringResource(R.string.settings_section_logs)) {
            SettingsRow(
                icon = Icons.Outlined.Description,
                iconColor = accentIndigo,
                title = stringResource(R.string.settings_section_logs),
                subtitle = stringResource(R.string.settings_logs_subtitle),
                onClick = onLogsClick,
                showDivider = false,
            )
        }

        // -- About --
        SettingsSection(header = stringResource(R.string.settings_section_about)) {
            SettingsRow(
                icon = Icons.Outlined.Info,
                iconColor = accentBlue,
                title = stringResource(R.string.settings_about_minis),
                subtitle = stringResource(R.string.settings_about_subtitle),
                onClick = onAboutClick,
            )
            SettingsRow(
                icon = Icons.Outlined.FrontHand,
                iconColor = accentTeal,
                title = stringResource(R.string.settings_privacy_policy),
                subtitle = null,
                onClick = { openExternalUrl(context, "https://openminis.github.io/privacy-policy.html") },
            )
            SettingsRow(
                icon = Icons.Outlined.Feedback,
                iconColor = accentAmber,
                title = stringResource(R.string.settings_feedback),
                subtitle = null,
                onClick = { showFeedbackSheet = true },
                showDivider = false,
            )
        }

        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = "Minis v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }

    if (showFeedbackSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFeedbackSheet = false },
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
            ),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_feedback),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ExpressiveTokens.OuterGroupRadius))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    FeedbackSheetItem(
                        icon = Icons.Outlined.BugReport,
                        iconColor = MaterialTheme.colorScheme.error,
                        title = stringResource(R.string.settings_submit_github_issues),
                        showDivider = true,
                        onClick = {
                            showFeedbackSheet = false
                            openExternalUrl(context, buildBugReportUrl())
                        },
                    )
                    FeedbackSheetItem(
                        icon = Icons.AutoMirrored.Outlined.Send,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.settings_feedback_telegram),
                        showDivider = true,
                        onClick = {
                            showFeedbackSheet = false
                            openExternalUrl(context, "https://t.me/+2NzhOJuzRyI1YmM1")
                        },
                    )
                    FeedbackSheetItem(
                        icon = Icons.Outlined.Email,
                        iconColor = MaterialTheme.colorScheme.tertiary,
                        title = stringResource(R.string.settings_feedback_email),
                        showDivider = false,
                        onClick = {
                            showFeedbackSheet = false
                            openExternalUrl(context, buildFeedbackMailto())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlCenterBentoGrid(
    onProvidersClick: () -> Unit,
    onModelGroupsClick: () -> Unit,
    onSoulClick: () -> Unit,
    onRootfsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BentoControlTile(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_manage_providers),
                subtitle = stringResource(R.string.settings_manage_providers_subtitle),
                icon = Icons.Outlined.Lock,
                badgeShape = MaterialShapes.Gem.toShape(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                badgeColor = MaterialTheme.colorScheme.primary,
                badgeIconColor = MaterialTheme.colorScheme.onPrimary,
                onClick = onProvidersClick,
            )
            BentoControlTile(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_model_groups),
                subtitle = stringResource(R.string.settings_model_groups_subtitle),
                icon = Icons.Outlined.Settings,
                badgeShape = MaterialShapes.Cookie9Sided.toShape(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                badgeColor = MaterialTheme.colorScheme.secondary,
                badgeIconColor = MaterialTheme.colorScheme.onSecondary,
                onClick = onModelGroupsClick,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BentoControlTile(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_soul),
                subtitle = stringResource(R.string.settings_soul_subtitle),
                icon = Icons.Outlined.AutoAwesome,
                badgeShape = MaterialShapes.Sunny.toShape(),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                badgeColor = MaterialTheme.colorScheme.tertiary,
                badgeIconColor = MaterialTheme.colorScheme.onTertiary,
                onClick = onSoulClick,
            )
            BentoControlTile(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_section_storage),
                subtitle = stringResource(R.string.settings_storage_subtitle),
                icon = Icons.Outlined.Inventory2,
                badgeShape = MaterialShapes.Pentagon.toShape(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                badgeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                badgeIconColor = MaterialTheme.colorScheme.primary,
                onClick = onRootfsClick,
            )
        }
    }
}

@Composable
private fun BentoControlTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeShape: Shape,
    containerColor: Color,
    contentColor: Color,
    badgeColor: Color,
    badgeIconColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 126.dp),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(badgeColor, badgeShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeIconColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FeedbackSheetItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val (badgeBg, badgeFg) = expressiveIconBadgeColors(iconColor)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(badgeBg, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeFg,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showDivider) {
            ExpressiveArticulatedSeam()
        }
    }
}

/**
 * Build the GitHub Issues "new bug report" URL with the body pre-filled
 * from the existing bug-report template.
 */
private fun buildBugReportUrl(): String {
    val osVersion = android.os.Build.VERSION.RELEASE
    val sdkInt = android.os.Build.VERSION.SDK_INT
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE
    val manufacturer = android.os.Build.MANUFACTURER
    val model = android.os.Build.MODEL

    val body = """
        ## 📝 Problem Summary

        <!-- Briefly describe the issue you encountered -->


        ## 📱 Basic Information

        | Field | Value |
        |-------|-------|
        | Platform | Android |
        | OS Version | Android $osVersion (API $sdkInt) |
        | Minis Version | $versionName (build $versionCode) |
        | Device Model | $manufacturer $model |

        ## 🔁 Steps to Reproduce

        1.
        2.
        3.

        ## ❌ Error Details

        ```
        paste error here
        ```

        ## ✅ Expected Behavior



        ## 🗂️ Additional Information

    """.trimIndent()

    val encodedBody = java.net.URLEncoder.encode(body, "UTF-8")
    val title = java.net.URLEncoder.encode("[Bug] ", "UTF-8")
    return "https://github.com/OpenMinis/OpenMinis/issues/new" +
        "?template=bug_report.md" +
        "&title=$title" +
        "&body=$encodedBody"
}

/**
 * Compose a `mailto:` URL with a prefilled subject and body that include
 * app version, Android version, and device model.
 */
private fun buildFeedbackMailto(): String {
    val body = """
        Please describe your feedback:


        ---
        App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
        Android Version: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})
        Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}

        Screenshot (optional): Please attach a screenshot if relevant.
    """.trimIndent()
    val subject = java.net.URLEncoder.encode("Minis Feedback", "UTF-8")
    val encodedBody = java.net.URLEncoder.encode(body, "UTF-8")
    return "mailto:dev@openminis.app?subject=$subject&body=$encodedBody"
}
