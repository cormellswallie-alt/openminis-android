package com.openminis.app.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.openminis.app.R
import com.openminis.app.data.repository.MCPRepository
import com.openminis.app.ui.settings.ExpressiveEmptyStateCard
import com.openminis.app.ui.settings.SettingsRow
import com.openminis.app.ui.settings.SettingsSection
import com.openminis.app.ui.settings.SettingsSwitch
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.toShape

private const val MCP_NOTE_TRUNC = 200

/**
 * Bottom sheet showing all configured MCP servers with per-session
 * enable/disable toggles.
 */
@Composable
fun SessionMcpsSheet(
    sessionId: String,
    mcpRepository: MCPRepository,
    onDismiss: () -> Unit,
) {
    val servers by mcpRepository.servers.collectAsState()

    val overrides = remember(servers) {
        mutableStateMapOf<String, Boolean>().apply {
            for (server in servers) {
                put(server.id, mcpRepository.isEnabledForSession(server.id, sessionId))
            }
        }
    }

    StandardChatSheet(
        title = stringResource(R.string.session_mcps_title),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (servers.isEmpty()) {
                ExpressiveEmptyStateCard(
                    icon = Icons.Outlined.Extension,
                    title = stringResource(R.string.session_mcps_empty_title),
                    subtitle = stringResource(R.string.session_mcps_empty_subtitle),
                    badgeShape = MaterialShapes.Gem.toShape(),
                )
            } else {
                SettingsSection(
                    footer = stringResource(R.string.session_mcps_footer),
                ) {
                    servers.forEachIndexed { index, server ->
                        val note = server.note?.trim().orEmpty()
                        val sub = note
                            .ifEmpty { server.transportSummary }
                            .let { if (it.length > MCP_NOTE_TRUNC) it.substring(0, MCP_NOTE_TRUNC) + "…" else it }
                        SettingsRow(
                            title = server.id,
                            subtitle = sub.takeIf { it.isNotBlank() },
                            showChevron = false,
                            showDivider = index < servers.size - 1,
                            trailing = {
                                SettingsSwitch(
                                    checked = overrides[server.id] ?: server.enabled,
                                    onCheckedChange = { enabled ->
                                        overrides[server.id] = enabled
                                        mcpRepository.setSessionOverride(sessionId, server.id, enabled)
                                    },
                                )
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(bottom = 24.dp))
            }
        }
    }
}
