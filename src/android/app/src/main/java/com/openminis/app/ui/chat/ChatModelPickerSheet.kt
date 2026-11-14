package com.openminis.app.ui.chat

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.model.ModelGroup
import com.openminis.app.data.model.ProviderConfig
import com.openminis.app.data.model.ProviderType
import com.openminis.app.data.model.RoutingStrategy
import com.openminis.app.data.model.SystemVoiceEntries
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.logging.AppLogger
import com.openminis.app.ui.components.ModalityBadge
import com.openminis.app.ui.components.ModelSearch
import com.openminis.app.ui.components.QuickTestSheet
import com.openminis.app.ui.components.modalityBadges
import com.openminis.app.ui.components.rememberDebouncedQuery
import com.openminis.app.ui.components.rememberSheetScrollGuard
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.ExpressiveTokens
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.articulatedItemShape
import com.openminis.app.ui.theme.toShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ModelPickerSheet(
    groups: List<ModelGroup>,
    selectedGroupId: String?,
    activeEntryId: String?,
    defaultPrimaryGroupId: String?,
    config: ProviderConfig,
    providerRepository: ProviderRepository,
    onSelectGroup: (String) -> Unit,
    onSelectGroupEntry: (String, String) -> Unit,
    onSelectEntry: (String) -> Unit,
    onDismiss: () -> Unit,
    onEditGroups: (() -> Unit)? = null,
    onEditProvider: ((instanceId: String) -> Unit)? = null,
) {
    val openTime = remember { System.nanoTime() }

    LaunchedEffect(Unit) {
        AppLogger.info("ModelPicker", "[ModelPicker] open triggered")
        withFrameNanos { frameTime ->
            val ms = (frameTime - openTime) / 1_000_000.0
            AppLogger.info("ModelPicker", "[ModelPicker] first frame rendered: ${"%.1f".format(ms)}ms (total since trigger)")
        }
    }

    var searchText by remember { mutableStateOf("") }
    var expandedGroupIds by remember { mutableStateOf(setOf<String>()) }
    val allInstanceIds = remember(config) {
        config.instances.filter { it.isEnabled }.map { it.id }.toSet()
    }
    var collapsedInstanceIds by remember(allInstanceIds) {
        mutableStateOf(allInstanceIds)
    }

    var quickTestEntry by remember { mutableStateOf<ModelEntry?>(null) }

    val query = rememberDebouncedQuery(searchText)

    val filteredGroups = remember(groups, query) {
        if (query.isEmpty()) groups
        else groups.filter { ModelSearch.score(it.name, query) > 0 }
    }

    val baseSections = remember(config) {
        val byInstance = config.modelEntries.asSequence()
            .filter { !it.isHidden }
            .groupBy { it.providerInstanceId }
        config.instances
            .filter { it.isEnabled }
            .map { it to (byInstance[it.id] ?: emptyList()) }
            .filter { it.second.isNotEmpty() }
    }
    fun sectionLabel(instance: com.openminis.app.data.model.ProviderInstance) =
        instance.label.ifEmpty { instance.providerType.displayName }

    var searchResult by remember {
        mutableStateOf<ModelSearch.Result<com.openminis.app.data.model.ProviderInstance>?>(null)
    }
    LaunchedEffect(baseSections, query) {
        if (query.isEmpty()) {
            searchResult = null
            return@LaunchedEffect
        }
        val t0 = System.nanoTime()
        val r = withContext(Dispatchers.Default) {
            ModelSearch.search(baseSections, ::sectionLabel, query)
        }
        searchResult = r
        AppLogger.info(
            "ModelPicker",
            "[ModelPicker] search len=${query.length} matches=${r.totalMatches} shown=${r.shown} " +
                "${"%.1f".format((System.nanoTime() - t0) / 1_000_000.0)}ms",
        )
    }
    val allInstancesWithEntries: List<Pair<com.openminis.app.data.model.ProviderInstance, List<ModelEntry>>> =
        if (query.isEmpty()) baseSections
        else searchResult?.sections?.map { it.key to it.entries } ?: baseSections
    val truncatedSearch = searchResult?.takeIf { query.isNotEmpty() && it.truncated }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(
            topStart = ExpressiveTokens.SheetTopRadius,
            topEnd = ExpressiveTokens.SheetTopRadius,
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        ),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding(),
        ) {
            // ── M3 Expressive Sheet Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialShapes.SoftBurst.toShape(),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.model_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.model_picker_done),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // ── M3 Expressive Tonal Pill Search Bar ──
            val searchInteraction = remember { MutableInteractionSource() }
            val searchFocused by searchInteraction.collectIsFocusedAsState()
            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
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
                            if (searchText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.model_picker_search_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                        if (searchText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                                    .clip(CircleShape)
                                    .clickable { searchText = "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.model_picker_search_clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                },
            )

            val pickerScrollGuard = rememberSheetScrollGuard()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .nestedScroll(pickerScrollGuard),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            ) {
                // ── Model Groups (M3 Expressive Articulated Bento Stack) ──
                if (filteredGroups.isNotEmpty()) {
                    item(key = "__groups_header__") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 22.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.model_picker_groups_section),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            if (onEditGroups != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            CircleShape,
                                        )
                                        .clip(CircleShape)
                                        .clickable(onClick = onEditGroups)
                                        .padding(horizontal = 11.dp, vertical = 5.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.model_picker_groups_edit),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }

                    item(key = "__groups_stack__") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.GroupItemGap),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            filteredGroups.forEachIndexed { index, group ->
                                val isSelected = group.id == selectedGroupId
                                val isDefault = group.id == defaultPrimaryGroupId
                                val isExpanded = expandedGroupIds.contains(group.id)
                                val strategyLabel = when (group.strategy) {
                                    RoutingStrategy.fallback -> "FB"
                                    RoutingStrategy.loadBalance -> "LB"
                                }
                                val resolvedEntry = group.memberEntryIds.firstNotNullOfOrNull { entryId ->
                                    config.modelEntries.find { it.id == entryId }
                                        ?: SystemVoiceEntries.resolve(entryId)
                                } ?: if (isSelected && activeEntryId != null) {
                                    config.modelEntries.find { it.id == activeEntryId }
                                } else null

                                val resolvedCount = group.memberEntryIds.count { entryId ->
                                    config.modelEntries.any { it.id == entryId } ||
                                        SystemVoiceEntries.resolve(entryId) != null
                                }

                                val groupShape = articulatedItemShape(index = index, count = filteredGroups.size)
                                val groupBg = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = if (ChatColors.isDark) 0.42f else 0.56f,
                                    )
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                                val chevronAngle by animateFloatAsState(
                                    targetValue = if (isExpanded) 180f else 0f,
                                    label = "groupChevron",
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(groupBg, groupShape)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.36f),
                                                    groupShape,
                                                )
                                            } else Modifier,
                                        )
                                        .clip(groupShape),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectGroup(group.id) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.secondaryContainer,
                                                    RoundedCornerShape(12.dp),
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Default.Layers,
                                                contentDescription = null,
                                                tint = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                },
                                                modifier = Modifier.size(19.dp),
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    text = group.name,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = 15.sp,
                                                        lineHeight = 20.sp,
                                                    ),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceContainerHighest,
                                                            CircleShape,
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                ) {
                                                    Icon(
                                                        if (group.strategy == RoutingStrategy.fallback)
                                                            Icons.Default.ArrowCircleDown
                                                        else Icons.Default.AccountTree,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(10.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(
                                                        strategyLabel,
                                                        fontSize = 9.5.sp,
                                                        lineHeight = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                if (isDefault) {
                                                    Text(
                                                        text = stringResource(R.string.model_picker_default_badge),
                                                        fontSize = 9.5.sp,
                                                        lineHeight = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier
                                                            .background(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                                                CircleShape,
                                                            )
                                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            if (resolvedEntry != null) {
                                                Text(
                                                    text = "→ ${resolvedEntry.model.displayName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            } else if (resolvedCount > 0) {
                                                Text(
                                                    text = pluralStringResource(
                                                        R.plurals.model_picker_models_count,
                                                        resolvedCount,
                                                        resolvedCount,
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                )
                                            } else {
                                                Text(
                                                    text = stringResource(
                                                        R.string.model_picker_models_count_unlinked,
                                                        group.memberEntryIds.size,
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                                            },
                                            modifier = Modifier.size(21.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    CircleShape,
                                                )
                                                .clip(CircleShape)
                                                .clickable {
                                                    expandedGroupIds = if (isExpanded) {
                                                        expandedGroupIds - group.id
                                                    } else {
                                                        expandedGroupIds + group.id
                                                    }
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(17.dp)
                                                    .graphicsLayer { rotationZ = chevronAngle },
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    // Expanded group members in a recessed inner container
                                    if (isExpanded) {
                                        val resolvedMembers = group.memberEntryIds.mapNotNull { entryId ->
                                            config.modelEntries.find { it.id == entryId }
                                                ?: SystemVoiceEntries.resolve(entryId)
                                        }
                                        val displayMembers = resolvedMembers.ifEmpty {
                                            if (isSelected && activeEntryId != null) {
                                                listOfNotNull(config.modelEntries.find { it.id == activeEntryId })
                                            } else emptyList()
                                        }
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                                    RoundedCornerShape(16.dp),
                                                )
                                                .border(
                                                    0.75.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
                                                    RoundedCornerShape(16.dp),
                                                )
                                                .clip(RoundedCornerShape(16.dp))
                                                .padding(6.dp),
                                        ) {
                                            if (displayMembers.isEmpty()) {
                                                Text(
                                                    text = stringResource(R.string.model_picker_no_linked_models),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                )
                                            }
                                            displayMembers.forEach { entry ->
                                                val isActive = activeEntryId == entry.id
                                                val instance = config.instances.find { it.id == entry.providerInstanceId }
                                                val memberShape = RoundedCornerShape(11.dp)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            if (isActive) {
                                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                                            } else {
                                                                Color.Transparent
                                                            },
                                                            memberShape,
                                                        )
                                                        .clip(memberShape)
                                                        .clickable { onSelectGroupEntry(group.id, entry.id) }
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(
                                                        if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                        contentDescription = null,
                                                        tint = if (isActive) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                        },
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                    Spacer(Modifier.width(9.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(7.dp)
                                                            .background(providerDotColor(instance?.providerType), CircleShape),
                                                    )
                                                    Spacer(Modifier.width(9.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        ModelNameWithActiveBadge(
                                                            entry.model.displayName,
                                                            showActiveBadge = isActive,
                                                        )
                                                        Row {
                                                            if (instance != null) {
                                                                Text(
                                                                    instance.label.ifEmpty { instance.providerType.displayName },
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                                                    maxLines = 1,
                                                                )
                                                                Text(
                                                                    " · ",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                                                )
                                                            }
                                                            Text(
                                                                entry.model.id,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.width(6.dp))
                                                    QuickTestButton(onClick = { quickTestEntry = entry })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (searchText.isEmpty()) {
                        item(key = "__groups_footer__") {
                            Text(
                                text = stringResource(R.string.model_picker_groups_footer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 10.dp),
                            )
                        }
                    }
                }

                // ── Individual Models by Provider (M3 Expressive Articulated Cards + Lazy Rows) ──
                if (allInstancesWithEntries.isNotEmpty()) {
                    allInstancesWithEntries.forEach { (instance, entries) ->
                        val isCollapsed = collapsedInstanceIds.contains(instance.id)
                        val dotColor = providerDotColor(instance.providerType)
                        val providerName = instance.label.ifEmpty { instance.providerType.displayName }

                        item(key = "section_${instance.id}") {
                            val headerShape = if (isCollapsed) {
                                RoundedCornerShape(ExpressiveTokens.OuterGroupRadius)
                            } else {
                                RoundedCornerShape(
                                    topStart = ExpressiveTokens.OuterGroupRadius,
                                    topEnd = ExpressiveTokens.OuterGroupRadius,
                                    bottomStart = ExpressiveTokens.InnerGroupRadius,
                                    bottomEnd = ExpressiveTokens.InnerGroupRadius,
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 8.dp,
                                        bottom = if (isCollapsed) 4.dp else ExpressiveTokens.GroupItemGap,
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, headerShape)
                                    .clip(headerShape),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            collapsedInstanceIds = if (isCollapsed) {
                                                collapsedInstanceIds - instance.id
                                            } else {
                                                collapsedInstanceIds + instance.id
                                            }
                                        }
                                        .padding(start = 14.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(
                                                dotColor.copy(alpha = if (ChatColors.isDark) 0.22f else 0.14f),
                                                RoundedCornerShape(11.dp),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .background(dotColor, CircleShape),
                                        )
                                    }
                                    Spacer(Modifier.width(11.dp))
                                    Text(
                                        text = providerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${entries.size}",
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                                CircleShape,
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                    if (onEditProvider != null) {
                                        Spacer(Modifier.width(6.dp))
                                        val editLabel = stringResource(R.string.model_picker_edit_provider, providerName)
                                        Text(
                                            text = stringResource(R.string.model_picker_groups_edit),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    CircleShape,
                                                )
                                                .clip(CircleShape)
                                                .clickable { onEditProvider(instance.id) }
                                                .semantics { contentDescription = editLabel }
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    val providerChevronAngle by animateFloatAsState(
                                        targetValue = if (isCollapsed) 0f else 180f,
                                        label = "providerChevron",
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(17.dp)
                                                .graphicsLayer { rotationZ = providerChevronAngle },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                if (isCollapsed) {
                                    val selectedEntry = entries.firstOrNull { it.id == activeEntryId && selectedGroupId == null }
                                    val displayEntry = selectedEntry ?: entries.firstOrNull()
                                    if (displayEntry != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(ExpressiveTokens.GroupItemGap)
                                                .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 50.dp)
                                                .background(
                                                    if (selectedEntry != null) {
                                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                                            alpha = if (ChatColors.isDark) 0.36f else 0.48f,
                                                        )
                                                    } else Color.Transparent,
                                                )
                                                .clickable { onSelectEntry(displayEntry.id) }
                                                .padding(horizontal = 16.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                if (selectedEntry != null) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (selectedEntry != null) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                                                },
                                                modifier = Modifier.size(20.dp),
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            FlowRow(
                                                modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                                itemVerticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    displayEntry.model.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (selectedEntry != null) FontWeight.Bold else FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                                modalityBadges(displayEntry.model).forEach { badge ->
                                                    ModalityBadge(badge)
                                                }
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            QuickTestButton(onClick = { quickTestEntry = displayEntry })
                                        }
                                        if (entries.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(ExpressiveTokens.GroupItemGap)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 40.dp)
                                                    .clickable { collapsedInstanceIds = collapsedInstanceIds - instance.id }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                            ) {
                                                Icon(
                                                    Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    pluralStringResource(
                                                        R.plurals.model_picker_show_models,
                                                        entries.size,
                                                        entries.size,
                                                    ),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!isCollapsed) {
                            itemsIndexed(entries, key = { _, e -> "entry_${instance.id}_${e.id}" }) { index, entry ->
                                val isLastRow = index == entries.size - 1
                                val isSelected = activeEntryId == entry.id && selectedGroupId == null
                                val rowShape = if (isLastRow) {
                                    RoundedCornerShape(
                                        topStart = ExpressiveTokens.InnerGroupRadius,
                                        topEnd = ExpressiveTokens.InnerGroupRadius,
                                        bottomStart = ExpressiveTokens.OuterGroupRadius,
                                        bottomEnd = ExpressiveTokens.OuterGroupRadius,
                                    )
                                } else {
                                    RoundedCornerShape(ExpressiveTokens.InnerGroupRadius)
                                }
                                val rowBg = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = if (ChatColors.isDark) 0.42f else 0.56f,
                                    )
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                                Row(
                                    modifier = Modifier
                                        .padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = if (isLastRow) 6.dp else ExpressiveTokens.GroupItemGap,
                                        )
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .background(rowBg, rowShape)
                                        .clip(rowShape)
                                        .clickable { onSelectEntry(entry.id) }
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                                        },
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        ModelNameWithActiveBadge(
                                            entry.model.displayName,
                                            showActiveBadge = selectedGroupId != null && activeEntryId == entry.id,
                                            isSelected = isSelected,
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
                                    Spacer(Modifier.width(8.dp))
                                    QuickTestButton(onClick = { quickTestEntry = entry })
                                }
                            }
                        }
                    }
                }

                truncatedSearch?.let { r ->
                    item(key = "__search_cap_footer__") {
                        Text(
                            stringResource(R.string.model_picker_search_capped, r.shown, r.totalMatches),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                        )
                    }
                }

                // ── Empty / No Results ──
                if (filteredGroups.isEmpty() && allInstancesWithEntries.isEmpty()) {
                    item(key = "__empty_state__") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        MaterialShapes.Cookie9Sided.toShape(),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (searchText.isNotEmpty()) Icons.Default.Search else Icons.Default.Memory,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(
                                    if (searchText.isNotEmpty()) R.string.model_picker_no_results
                                    else R.string.model_picker_no_models,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(
                                    if (searchText.isEmpty()) R.string.model_picker_configure_hint
                                    else R.string.model_picker_try_different_search,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }

    quickTestEntry?.let { entry ->
        key(entry.id) {
            QuickTestSheet(
                entry = entry,
                providerRepository = providerRepository,
                onDismiss = { quickTestEntry = null },
            )
        }
    }
}

@Composable
private fun ModelNameWithActiveBadge(
    name: String,
    showActiveBadge: Boolean,
    isSelected: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || showActiveBadge) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (showActiveBadge) {
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.model_picker_active_badge),
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        CircleShape,
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun QuickTestButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Bolt,
            contentDescription = stringResource(R.string.model_picker_quick_test),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun providerDotColor(providerType: ProviderType?): Color = when (providerType) {
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
