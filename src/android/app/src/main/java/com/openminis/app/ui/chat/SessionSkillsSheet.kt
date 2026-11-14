package com.openminis.app.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.data.repository.SkillRepository
import com.openminis.app.ui.components.DialogTextField
import com.openminis.app.ui.components.MinisSmallButton
import com.openminis.app.ui.components.MinisSmallOutlinedButton
import com.openminis.app.ui.settings.ExpressiveEmptyStateCard
import com.openminis.app.ui.settings.SettingsSection
import com.openminis.app.ui.settings.SkillRowItem
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.toShape

/**
 * Bottom sheet showing all skills with per-session enable/disable toggles.
 * Upgraded to Material 3 Expressive empty-state cards and tonal pill bulk actions.
 */
@Composable
fun SessionSkillsSheet(
    sessionId: String,
    skillRepository: SkillRepository,
    onDismiss: () -> Unit,
) {
    val skills by skillRepository.skills.collectAsState()

    val overrides = remember(skills) {
        mutableStateMapOf<String, Boolean>().apply {
            for (skill in skills) {
                put(skill.id, skillRepository.isEnabledForSession(skill.id, sessionId))
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredSkills by remember(skills) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) skills
            else skills.filter {
                it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
            }
        }
    }

    StandardChatSheet(
        title = stringResource(R.string.session_skills_title),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (skills.isEmpty()) {
                ExpressiveEmptyStateCard(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.session_skills_empty_title),
                    subtitle = stringResource(R.string.session_skills_empty_subtitle),
                    badgeShape = MaterialShapes.Clover4Leaf.toShape(),
                )
            } else {
                DialogTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = stringResource(R.string.skills_search_placeholder),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 24.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.session_skills_header_installed),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    MinisSmallButton(
                        onClick = {
                            for (skill in filteredSkills) {
                                overrides[skill.id] = true
                                skillRepository.setSessionOverride(sessionId, skill.id, true)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        elevation = null,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            stringResource(R.string.session_skills_enable_all),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    MinisSmallOutlinedButton(
                        onClick = {
                            for (skill in filteredSkills) {
                                overrides[skill.id] = false
                                skillRepository.setSessionOverride(sessionId, skill.id, false)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            stringResource(R.string.session_skills_disable_all),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                if (filteredSkills.isEmpty()) {
                    ExpressiveEmptyStateCard(
                        icon = Icons.Outlined.SearchOff,
                        title = stringResource(R.string.skills_search_no_match, searchQuery),
                        badgeShape = MaterialShapes.SoftBurst.toShape(),
                    )
                } else {
                    SettingsSection(
                        footer = stringResource(R.string.session_skills_footer),
                    ) {
                        filteredSkills.forEachIndexed { index, skill ->
                            SkillRowItem(
                                name = skill.name,
                                description = skill.description,
                                importSource = skill.importSource,
                                isEnabled = overrides[skill.id] ?: skill.isEnabled,
                                onToggle = { enabled ->
                                    overrides[skill.id] = enabled
                                    skillRepository.setSessionOverride(sessionId, skill.id, enabled)
                                },
                                showDivider = index < filteredSkills.size - 1,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(bottom = 24.dp))
            }
        }
    }
}
