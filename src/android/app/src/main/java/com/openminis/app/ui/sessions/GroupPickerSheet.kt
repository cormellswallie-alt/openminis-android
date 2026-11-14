package com.openminis.app.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openminis.app.R
import com.openminis.app.data.db.FolderEntity
import com.openminis.app.ui.chat.StandardChatSheetHeader
import com.openminis.app.ui.components.ExpressiveArticulatedSeam
import com.openminis.app.ui.components.MinisSmallButton
import com.openminis.app.ui.components.SectionDesign
import com.openminis.app.ui.components.SectionTextField
import com.openminis.app.ui.theme.ExpressiveLoadingIndicator

sealed interface GroupChoice {
    data class Existing(val folderId: String) : GroupChoice
    data class Create(val name: String, val description: String?) : GroupChoice
    data object RemoveFromGroup : GroupChoice
}

internal fun groupPickerOrder(folders: List<FolderEntity>, lastActivity: Map<String, Long>): List<FolderEntity> =
    folders.sortedByDescending { f ->
        maxOf(f.updatedAt, f.createdAt, lastActivity[f.id] ?: Long.MIN_VALUE)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPickerSheet(
    folders: List<FolderEntity>,
    memberCounts: Map<String, Int>,
    sessionCount: Int,
    anyFiled: Boolean,
    onChoose: (GroupChoice) -> Unit,
    onDismiss: () -> Unit,
    suggesting: Boolean = false,
    suggestFailed: Boolean = false,
    suggestion: SessionListViewModel.GroupSuggestion? = null,
    onSuggest: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    val createSuggestion = suggestion as? SessionListViewModel.GroupSuggestion.Create
    LaunchedEffect(createSuggestion) {
        if (createSuggestion != null) {
            newName = createSuggestion.name
            createSuggestion.description?.let { newDesc = it.take(FolderEntity.DESC_MAX_CHARS) }
        }
    }

    val trimmedName = newName.trim()
    val duplicate = remember(trimmedName, folders) {
        if (trimmedName.isEmpty()) null
        else folders.firstOrNull { it.name.trim().equals(trimmedName, ignoreCase = true) }
    }

    val title = when {
        sessionCount > 1 && anyFiled -> stringResource(R.string.group_picker_title_change_n, sessionCount)
        sessionCount > 1 -> stringResource(R.string.group_picker_title_move_n, sessionCount)
        anyFiled -> stringResource(R.string.group_picker_title_change)
        else -> stringResource(R.string.group_picker_title_move)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            StandardChatSheetHeader(
                title = title,
                onDismiss = onDismiss,
            )
            Spacer(Modifier.height(10.dp))

            // ── Create a new group inline ──────────────────────────────────
            SectionTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = stringResource(R.string.group_new_name_hint),
                modifier = Modifier.padding(horizontal = 20.dp),
                containerColor = SectionDesign.screenBackgroundColor(),
                contentHorizontalPadding = 16.dp,
            )
            Spacer(Modifier.height(8.dp))
            SectionTextField(
                value = newDesc,
                onValueChange = { newDesc = it.take(FolderEntity.DESC_MAX_CHARS) },
                placeholder = stringResource(R.string.group_desc_hint),
                modifier = Modifier.padding(horizontal = 20.dp),
                containerColor = SectionDesign.screenBackgroundColor(),
                contentHorizontalPadding = 16.dp,
            )

            if (duplicate != null) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
                        .clickable { onChoose(GroupChoice.Existing(duplicate.id)) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.group_duplicate_exists, duplicate.name),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            stringResource(R.string.group_duplicate_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            (suggestion as? SessionListViewModel.GroupSuggestion.Merge)?.let { merge ->
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                        .clickable { onChoose(GroupChoice.Existing(merge.folderId)) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        stringResource(R.string.group_suggest_merge, merge.folderName),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onSuggest != null) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable(enabled = !suggesting) { onSuggest() }
                            .padding(vertical = 7.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (suggesting) {
                            ExpressiveLoadingIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(
                            stringResource(
                                if (suggestFailed) R.string.group_suggest_failed
                                else R.string.group_suggest,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                MinisSmallButton(
                    onClick = {
                        onChoose(
                            GroupChoice.Create(
                                name = trimmedName,
                                description = newDesc.trim().ifBlank { null },
                            ),
                        )
                    },
                    enabled = trimmedName.isNotEmpty() && duplicate == null,
                ) { Text(stringResource(R.string.group_create), fontWeight = FontWeight.SemiBold) }
            }

            if (folders.isNotEmpty() || anyFiled) {
                Text(
                    text = stringResource(R.string.group_section_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 6.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    if (anyFiled) {
                        item(key = "no_group") {
                            val label = stringResource(R.string.group_none)
                            val hint = stringResource(R.string.group_none_subtitle)
                            GroupRow(
                                title = label,
                                subtitle = hint,
                                icon = Icons.Default.FolderOff,
                                onClick = { onChoose(GroupChoice.RemoveFromGroup) },
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = label
                                    stateDescription = hint
                                },
                            )
                            if (folders.isNotEmpty()) {
                                ExpressiveArticulatedSeam()
                            }
                        }
                    }
                    itemsIndexed(folders, key = { _, it -> it.id }) { index, folder ->
                        val count = memberCounts[folder.id] ?: 0
                        GroupRow(
                            title = folder.name,
                            subtitle = folder.description?.takeIf { it.isNotBlank() }
                                ?: if (count > 0) stringResource(R.string.group_n_chats, count)
                                else stringResource(R.string.group_empty),
                            icon = Icons.Default.Folder,
                            onClick = { onChoose(GroupChoice.Existing(folder.id)) },
                        )
                        if (index < folders.size - 1) {
                            ExpressiveArticulatedSeam()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
