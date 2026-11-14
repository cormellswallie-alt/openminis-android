package com.openminis.app.ui.sandbox

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import com.openminis.app.ui.theme.ExpressiveLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openminis.app.R
import com.openminis.app.ui.components.MinisAlertDialog
import com.openminis.app.ui.components.SettingsSection
import com.openminis.app.ui.settings.SettingsRow
import com.openminis.app.ui.settings.SettingsScaffold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RootfsManagementScreen(
    onBack: () -> Unit,
    onBrowseFiles: () -> Unit,
    onMirrorCategoryClick: (MirrorCategory) -> Unit = {},
    viewModel: RootfsManagementViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showResetDialog by remember { mutableStateOf(false) }
    var showResetBackupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh(context)
    }

    SettingsScaffold(
        title = stringResource(R.string.rootfs_management_title),
        onBack = onBack,
    ) {
        // --- Expressive Linux Sandbox Hero Bento Card ---
        SandboxHeroBentoCard(
            isInstalled = state.isInstalled,
            rootfsSize = state.rootfsSize,
            rootfsPath = state.rootfsPath,
        )

        // --- Live Processing Banner ---
        if (state.isProcessing) {
            SettingsSection(title = stringResource(R.string.rootfs_progress_section)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ExpressiveLoadingIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    state.installProgress?.let { p ->
                        LinearProgressIndicator(
                            progress = { p },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                        )
                    }
                }
            }
        }

        state.resultMessage?.let { msg ->
            SettingsSection(title = stringResource(R.string.rootfs_result_section)) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (state.lastOperationSuccess) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(18.dp),
                )
            }
        }

        if (state.isInstalled) {
            // --- Browse section ---
            SettingsSection(title = stringResource(R.string.rootfs_browse_section)) {
                SettingsRow(
                    icon = Icons.Filled.Folder,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.rootfs_browse_files_label),
                    onClick = onBrowseFiles,
                    showDivider = false,
                )
            }

            // --- Mirrors section ---
            SettingsSection(title = stringResource(R.string.rootfs_mirrors_section)) {
                MirrorsSectionView(onNavigate = onMirrorCategoryClick)
            }
        }

        // --- Container Network section ---
        SettingsSection(title = stringResource(R.string.rootfs_network_section)) {
            ContainerNetworkSection()
        }

        // --- Actions section ---
        SettingsSection(title = stringResource(R.string.rootfs_actions_section)) {
            if (!state.isInstalled) {
                SettingsRow(
                    icon = Icons.Filled.Download,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.rootfs_install_label),
                    onClick = if (!state.isProcessing) {
                        { viewModel.install(context) }
                    } else {
                        null
                    },
                    showChevron = !state.isProcessing,
                    showDivider = state.hasBackup,
                )
            } else {
                SettingsRow(
                    icon = Icons.Filled.Refresh,
                    iconColor = MaterialTheme.colorScheme.error,
                    title = stringResource(R.string.rootfs_reset_label),
                    subtitle = stringResource(R.string.rootfs_reset_description),
                    onClick = if (!state.isProcessing) {
                        { showResetDialog = true }
                    } else {
                        null
                    },
                    showChevron = false,
                    showDivider = true,
                )
                SettingsRow(
                    icon = Icons.Filled.Archive,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    title = stringResource(R.string.rootfs_reset_backup_label),
                    subtitle = stringResource(R.string.rootfs_reset_backup_description),
                    onClick = if (!state.isProcessing) {
                        { showResetBackupDialog = true }
                    } else {
                        null
                    },
                    showChevron = false,
                    showDivider = state.hasBackup,
                )
            }

            if (state.hasBackup) {
                SettingsRow(
                    icon = Icons.Filled.Restore,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = stringResource(R.string.rootfs_restore_label),
                    subtitle = stringResource(R.string.rootfs_restore_description),
                    onClick = if (!state.isProcessing && state.isInstalled) {
                        { viewModel.restoreBackup(context) }
                    } else {
                        null
                    },
                    showChevron = false,
                    showDivider = false,
                )
            }
        }

        // --- Info section ---
        SettingsSection(title = stringResource(R.string.rootfs_info_section)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.rootfs_about_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The rootfs contains the Alpine Linux filesystem used by " +
                        "the sandbox. Resetting will delete all data and restore to " +
                        "factory state.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reset: Delete everything\n" +
                        "Backup: Save /root directory\n" +
                        "Restore: Recover saved data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showResetDialog) {
        MinisAlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = stringResource(R.string.rootfs_reset_confirm_title),
            text = stringResource(R.string.rootfs_reset_confirm_message),
            confirmText = "Reset",
            isDestructive = true,
            onConfirm = {
                showResetDialog = false
                viewModel.resetRootfs(context, keepUserData = false)
            },
            dismissText = stringResource(R.string.common_cancel),
        )
    }

    if (showResetBackupDialog) {
        MinisAlertDialog(
            onDismissRequest = { showResetBackupDialog = false },
            title = stringResource(R.string.rootfs_reset_backup_confirm_title),
            text = stringResource(R.string.rootfs_reset_backup_confirm_message),
            confirmText = stringResource(R.string.rootfs_reset_backup_confirm_button),
            isDestructive = true,
            onConfirm = {
                showResetBackupDialog = false
                viewModel.resetRootfs(context, keepUserData = true)
            },
            dismissText = stringResource(R.string.common_cancel),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SandboxHeroBentoCard(
    isInstalled: Boolean,
    rootfsSize: Long,
    rootfsPath: String,
) {
    val context = LocalContext.current
    val containerColor = if (isInstalled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isInstalled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialShapes.Pentagon.toShape())
                            .background(
                                if (isInstalled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = if (isInstalled) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(23.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.rootfs_status_section),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor.copy(alpha = 0.75f),
                        )
                        Text(
                            text = "Alpine Linux Sandbox",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColor,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            if (isInstalled) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = if (isInstalled) contentColor else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp),
                        )
                        if (isInstalled && rootfsSize > 0) {
                            Text(
                                text = Formatter.formatFileSize(context, rootfsSize),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                            )
                        } else if (isInstalled) {
                            ExpressiveLoadingIndicator(
                                modifier = Modifier.size(16.dp),
                                color = contentColor,
                            )
                        }
                    }
                }
            }

            if (isInstalled && rootfsPath.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = contentColor.copy(alpha = 0.08f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.rootfs_path_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor.copy(alpha = 0.72f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = rootfsPath,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
