package com.openminis.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.data.model.LLMModel
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.model.ProviderInstance
import com.openminis.app.data.model.ProviderType
import com.openminis.app.data.model.SystemVoiceEntries
import com.openminis.app.data.model.hasImageInput
import com.openminis.app.data.model.isVoiceInputCandidate
import com.openminis.app.data.model.isVoiceOutputCandidate
import com.openminis.app.data.model.normalizeModalities
import com.openminis.app.data.repository.ModelEntryRanking
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.ExpressiveTokens

enum class PickerModalityFilter {
    AUDIO_INPUT,
    AUDIO_OUTPUT,
    IMAGE_INPUT;

    fun matches(model: LLMModel): Boolean = when (this) {
        AUDIO_INPUT -> model.isVoiceInputCandidate
        AUDIO_OUTPUT -> model.isVoiceOutputCandidate
        IMAGE_INPUT -> model.hasImageInput
    }

    fun systemEntries(): List<ModelEntry> = when (this) {
        AUDIO_INPUT -> listOf(SystemVoiceEntries.asrOnline, SystemVoiceEntries.asrOffline)
        AUDIO_OUTPUT -> listOf(SystemVoiceEntries.tts)
        IMAGE_INPUT -> emptyList()
    }
}

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.modelEntryPickerItems(
    instances: List<ProviderInstance>,
    availableEntries: List<ModelEntry>,
    selectedIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    searchQuery: MutableState<String>,
    collapsedInstanceIds: MutableState<Set<String>>,
    @StringRes emptyTextRes: Int,
    @StringRes emptySearchTextRes: Int,
    @StringRes searchPlaceholderRes: Int,
    @StringRes clearContentDescriptionRes: Int,
    onQuickTest: ((ModelEntry) -> Unit)? = null,
    modalityFilter: PickerModalityFilter? = null,
    excludeIds: Set<String> = emptySet(),
    systemProviderLabel: String = "System",
    sections: ModelSearch.Result<ProviderInstance>? = null,
) {
    val result = sections ?: modelEntryPickerSearch(
        instances, availableEntries, ModelSearch.normalize(searchQuery.value),
        modalityFilter, excludeIds, systemProviderLabel,
    )
    val isSearching = searchQuery.value.isNotBlank()
    val instanceWithEntries = result.sections.map { it.key to it.entries }
    val effectiveCollapsed = if (isSearching) emptySet() else collapsedInstanceIds.value

    item("__search__") {
        val searchInteraction = remember { MutableInteractionSource() }
        val searchFocused by searchInteraction.collectIsFocusedAsState()
        BasicTextField(
            value = searchQuery.value,
            onValueChange = { searchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(46.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = searchInteraction,
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            CircleShape,
                        )
                        .then(
                            if (searchFocused) {
                                Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier.border(
                                    0.75.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    CircleShape,
                                )
                            },
                        )
                        .padding(start = 15.dp, end = 10.dp),
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = if (searchFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (searchQuery.value.isEmpty()) {
                            Text(
                                text = stringResource(searchPlaceholderRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                    if (searchQuery.value.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                                .clip(CircleShape)
                                .clickable { searchQuery.value = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(clearContentDescriptionRes),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            },
        )
    }

    instanceWithEntries.forEach { (instance, entries) ->
        val isCollapsed = instance.id in effectiveCollapsed
        val dotColor = providerDotColor(instance.providerType)

        item(key = "header_${instance.id}") {
            val chevronRotation by animateFloatAsState(
                targetValue = if (isCollapsed) 0f else 180f,
                label = "entryPickerChevron",
            )
            val headerShape = RoundedCornerShape(
                topStart = ExpressiveTokens.OuterGroupRadius,
                topEnd = ExpressiveTokens.OuterGroupRadius,
                bottomStart = ExpressiveTokens.InnerGroupRadius,
                bottomEnd = ExpressiveTokens.InnerGroupRadius,
            )
            Row(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = ExpressiveTokens.GroupItemGap,
                    )
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, headerShape)
                    .clip(headerShape)
                    .clickable {
                        collapsedInstanceIds.value = if (isCollapsed) {
                            collapsedInstanceIds.value - instance.id
                        } else {
                            collapsedInstanceIds.value + instance.id
                        }
                    }
                    .padding(start = 14.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            dotColor.copy(alpha = if (ChatColors.isDark) 0.22f else 0.14f),
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(dotColor, CircleShape),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    instance.label.ifEmpty { instance.providerType.displayName },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${entries.size}",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = chevronRotation },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (isCollapsed) {
            item(key = "collapsed_${instance.id}") {
                val firstEntry = entries.firstOrNull() ?: return@item
                val isSelected = firstEntry.id in selectedIds
                val bottomShape = RoundedCornerShape(
                    topStart = ExpressiveTokens.InnerGroupRadius,
                    topEnd = ExpressiveTokens.InnerGroupRadius,
                    bottomStart = ExpressiveTokens.OuterGroupRadius,
                    bottomEnd = ExpressiveTokens.OuterGroupRadius,
                )
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                        .fillMaxWidth()
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = if (ChatColors.isDark) 0.42f else 0.56f,
                                )
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            bottomShape,
                        )
                        .clip(bottomShape)
                        .clickable { onToggleSelection(firstEntry.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectionDot(isSelected)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        firstEntry.model.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        pluralStringResource(
                            R.plurals.model_picker_models_count,
                            entries.size,
                            entries.size,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            itemsIndexed(entries, key = { _, e -> "entry_${instance.id}_${e.id}" }) { index, entry ->
                val isLast = index == entries.size - 1
                val isSelected = entry.id in selectedIds
                val rowShape = if (isLast) {
                    RoundedCornerShape(
                        topStart = ExpressiveTokens.InnerGroupRadius,
                        topEnd = ExpressiveTokens.InnerGroupRadius,
                        bottomStart = ExpressiveTokens.OuterGroupRadius,
                        bottomEnd = ExpressiveTokens.OuterGroupRadius,
                    )
                } else {
                    RoundedCornerShape(ExpressiveTokens.InnerGroupRadius)
                }
                Row(
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = if (isLast) 4.dp else ExpressiveTokens.GroupItemGap,
                        )
                        .fillMaxWidth()
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = if (ChatColors.isDark) 0.42f else 0.56f,
                                )
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            rowShape,
                        )
                        .clip(rowShape)
                        .clickable { onToggleSelection(entry.id) }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectionDot(isSelected)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.model.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                entry.model.id,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            )
                            modalityBadges(entry.model).forEach { badge ->
                                ModalityBadge(badge)
                            }
                        }
                    }
                    if (onQuickTest != null && !SystemVoiceEntries.isSystemEntryId(entry.id)) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(10.dp),
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onQuickTest(entry) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Bolt,
                                contentDescription = stringResource(R.string.quicktest_button),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (isSearching && result.truncated) {
        item("__search_cap_footer__") {
            Text(
                stringResource(R.string.model_picker_search_capped, result.shown, result.totalMatches),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }

    if (instanceWithEntries.isEmpty()) {
        item("__empty__") {
            Text(
                stringResource(if (isSearching) emptySearchTextRes else emptyTextRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
            )
        }
    }
}

@Composable
private fun SelectionDot(isSelected: Boolean) {
    Icon(
        if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = null,
        tint = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
        modifier = Modifier.size(20.dp),
    )
}

fun modalityBadges(model: LLMModel): List<String> {
    val ins = model.inputModalities.normalizeModalities() ?: emptyList()
    val outs = model.outputModalities.normalizeModalities() ?: emptyList()
    val badges = mutableListOf<String>()
    if ("image" in ins) badges.add("img")
    if ("audio" in ins) badges.add("audio")
    if ("video" in ins) badges.add("video")
    if ("pdf" in ins) badges.add("pdf")
    if ("image" in outs) badges.add("img-out")
    if ("audio" in outs) badges.add("audio-out")
    if ("video" in outs) badges.add("video-out")
    return badges
}

@Composable
fun ModalityBadge(badge: String) {
    Text(
        badge,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer.copy(
                    alpha = if (ChatColors.isDark) 0.55f else 0.72f,
                ),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

fun providerDotColor(providerType: ProviderType?): Color = when (providerType) {
    ProviderType.anthropic -> Color(0xFFAB47BC)
    ProviderType.gemini -> Color(0xFF42A5F5)
    ProviderType.openAI -> Color(0xFF4CAF50)
    ProviderType.openRouter -> Color(0xFF00BCD4)
    ProviderType.xAI -> Color(0xFFFF7043)
    ProviderType.kimiCode -> Color(0xFF5C6BC0)
    ProviderType.githubCopilot -> Color(0xFF6E5494)
    ProviderType.openAIResponses -> Color(0xFF4CAF50)
    ProviderType.antigravity,
    ProviderType.unsupported -> Color(0xFF79747E)
    null -> Color(0xFF79747E)
}

@Composable
fun rememberModelEntryPickerSections(
    instances: List<ProviderInstance>,
    availableEntries: List<ModelEntry>,
    searchQuery: String,
    modalityFilter: PickerModalityFilter? = null,
    excludeIds: Set<String> = emptySet(),
    systemProviderLabel: String = "System",
): ModelSearch.Result<ProviderInstance> {
    val query = rememberDebouncedQuery(searchQuery)
    return remember(instances, availableEntries, query, modalityFilter, excludeIds, systemProviderLabel) {
        modelEntryPickerSearch(instances, availableEntries, query, modalityFilter, excludeIds, systemProviderLabel)
    }
}

internal fun modelEntryPickerSearch(
    instances: List<ProviderInstance>,
    availableEntries: List<ModelEntry>,
    query: String,
    modalityFilter: PickerModalityFilter?,
    excludeIds: Set<String>,
    systemProviderLabel: String,
): ModelSearch.Result<ProviderInstance> {
    val systemPair: Pair<ProviderInstance, List<ModelEntry>>? = modalityFilter
        ?.systemEntries()
        ?.filter { it.id !in excludeIds }
        ?.takeIf { it.isNotEmpty() }
        ?.let { SystemVoiceEntries.syntheticInstance(systemProviderLabel) to it }

    val byInstance = availableEntries
        .filter { modalityFilter == null || modalityFilter.matches(it.model) }
        .groupBy { it.providerInstanceId }
    val base = listOfNotNull(systemPair) + instances
        .filter { it.isEnabled && !SystemVoiceEntries.isSystemEntryId(it.id) }
        .mapNotNull { instance ->
            val entries = byInstance[instance.id] ?: return@mapNotNull null
            instance to ModelEntryRanking.sortedByReleaseRank(entries)
        }
    return ModelSearch.search(base, { it.label.ifEmpty { it.providerType.displayName } }, query)
}
