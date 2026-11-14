package com.openminis.app.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.model.ModelGroup
import com.openminis.app.data.model.SystemVoiceEntries
import com.openminis.app.data.model.SystemVoiceIds
import com.openminis.app.data.repository.ModelEntryRanking
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.speech.SystemTtsVoiceCatalog
import com.openminis.app.speech.VoiceOutputState
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.ExpressiveTokens
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.articulatedItemShape
import com.openminis.app.ui.theme.toShape

/**
 * [T-android-unified-model-picker] M3 Expressive single-select model picker sheet
 * shared across voice input, voice output, and vision model pickers.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnifiedModelPickerSheet(
    providerRepository: ProviderRepository,
    title: String,
    modalityFilter: PickerModalityFilter,
    boundGroup: ModelGroup?,
    boundGroupName: String?,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    onEditGroups: (() -> Unit)? = null,
    onEditProvider: ((instanceId: String) -> Unit)? = null,
) {
    val config by providerRepository.config.collectAsState()
    var quickTestEntry by remember { mutableStateOf<ModelEntry?>(null) }
    var searchText by remember { mutableStateOf("") }
    var groupExpanded by remember { mutableStateOf(false) }
    val pickerContext = LocalContext.current
    var systemVoices by remember {
        mutableStateOf(emptyList<SystemTtsVoiceCatalog.SystemVoice>())
    }
    val pickedSystemVoice by VoiceOutputState.systemVoiceName.collectAsState()
    LaunchedEffect(modalityFilter) {
        if (modalityFilter == PickerModalityFilter.AUDIO_OUTPUT) {
            VoiceOutputState.init(pickerContext)
            systemVoices = SystemTtsVoiceCatalog.voices(pickerContext)
        }
    }
    var collapsedInstanceIds by remember { mutableStateOf(setOf<String>()) }

    fun select(id: String?) {
        onSelect(id)
        onDismiss()
    }

    val query = rememberDebouncedQuery(searchText)
    fun matches(text: String): Boolean = query.isEmpty() || ModelSearch.score(text, query) > 0

    val activeGroupMemberId = remember(config, modalityFilter) {
        when (modalityFilter) {
            PickerModalityFilter.AUDIO_INPUT ->
                providerRepository.activeVoiceGroupMemberId(output = false)
            PickerModalityFilter.AUDIO_OUTPUT ->
                providerRepository.activeVoiceGroupMemberId(output = true)
            else -> null
        }
    }

    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val cardShape = RoundedCornerShape(ExpressiveTokens.OuterGroupRadius)

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
                        imageVector = if (modalityFilter == PickerModalityFilter.IMAGE_INPUT) {
                            Icons.Default.Layers
                        } else {
                            Icons.Outlined.GraphicEq
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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

            val unifiedPickerScrollGuard = rememberSheetScrollGuard()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .nestedScroll(unifiedPickerScrollGuard),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            ) {
                // ── Bound Model Group Card ──
                if (boundGroup != null && matches(boundGroupName ?: "")) {
                    item("group_header") {
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

                    item("group_card") {
                        val groupSelected =
                            selectedId == null || boundGroup.memberEntryIds.contains(selectedId)
                        val groupBg = if (groupSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = if (ChatColors.isDark) 0.42f else 0.56f,
                            )
                        } else {
                            cardColor
                        }
                        val groupChevron by animateFloatAsState(
                            targetValue = if (groupExpanded) 180f else 0f,
                            label = "unifiedGroupChevron",
                        )
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .background(groupBg, cardShape)
                                .then(
                                    if (groupSelected) {
                                        Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.36f),
                                            cardShape,
                                        )
                                    } else Modifier,
                                )
                                .clip(cardShape),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { select(null) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (groupSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondaryContainer,
                                            RoundedCornerShape(12.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = if (groupSelected) {
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
                                            boundGroupName ?: "",
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
                                                Icons.Default.ArrowCircleDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(10.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                "FB",
                                                fontSize = 9.5.sp,
                                                lineHeight = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        stringResource(
                                            R.string.voice_input_picker_group_models,
                                            boundGroup.memberEntryIds.count { id ->
                                                config.modelEntries.any { it.id == id } ||
                                                    SystemVoiceEntries.resolve(id) != null
                                            },
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                SelectionMark(selected = groupSelected)
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHighest,
                                            CircleShape,
                                        )
                                        .clip(CircleShape)
                                        .clickable { groupExpanded = !groupExpanded },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .graphicsLayer { rotationZ = groupChevron },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (groupExpanded) {
                                val members = boundGroup.memberEntryIds.mapNotNull { id ->
                                    config.modelEntries.find { it.id == id }
                                        ?: SystemVoiceEntries.resolve(id)
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
                                    members.forEach { entry ->
                                        val inst = config.instances.find { it.id == entry.providerInstanceId }
                                        val isActive = if (selectedId != null) {
                                            selectedId == entry.id
                                        } else {
                                            entry.id == activeGroupMemberId
                                        }
                                        val memberShape = RoundedCornerShape(11.dp)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isActive) {
                                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                                    } else Color.Transparent,
                                                    memberShape,
                                                )
                                                .clip(memberShape)
                                                .clickable { select(entry.id) }
                                                .padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            SelectionMark(selected = isActive)
                                            Spacer(Modifier.width(9.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .background(
                                                        providerDotColor(inst?.providerType),
                                                        CircleShape,
                                                    ),
                                            )
                                            Spacer(Modifier.width(9.dp))
                                            Text(
                                                entry.model.displayName.ifBlank { entry.model.id },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (isActive && selectedId == null) {
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
                                }
                            }
                        }
                    }
                    item("group_footer") {
                        Text(
                            stringResource(R.string.voice_input_picker_group_footer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
                        )
                    }
                }

                // ── System Voice Card ──
                val systemEntries = modalityFilter.systemEntries()
                    .filter { matches(it.model.displayName) }
                val filteredSystemVoices = if (modalityFilter == PickerModalityFilter.AUDIO_OUTPUT) {
                    systemVoices.filter { matches(it.label) }
                } else emptyList()

                if (systemEntries.isNotEmpty() || filteredSystemVoices.isNotEmpty()) {
                    item("system_card") {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.GroupItemGap),
                        ) {
                            Text(
                                text = stringResource(R.string.voice_input_picker_system),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp),
                            )
                            val totalRows = systemEntries.size + filteredSystemVoices.size
                            systemEntries.forEachIndexed { i, entry ->
                                val systemInEffect =
                                    (if (selectedId != null) selectedId == entry.id
                                    else entry.id == activeGroupMemberId) &&
                                        (modalityFilter != PickerModalityFilter.AUDIO_OUTPUT ||
                                            pickedSystemVoice == null)
                                val rowShape = articulatedItemShape(index = i, count = totalRows)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (systemInEffect) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = if (ChatColors.isDark) 0.42f else 0.56f,
                                                )
                                            } else cardColor,
                                            rowShape,
                                        )
                                        .clip(rowShape)
                                        .clickable {
                                            if (modalityFilter == PickerModalityFilter.AUDIO_OUTPUT) {
                                                VoiceOutputState.setSystemVoice(null, null)
                                            }
                                            select(entry.id)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SelectionMark(selected = systemInEffect)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            entry.model.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            systemEntrySubtitle(entry),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (systemInEffect && selectedId == null) {
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
                            filteredSystemVoices.forEachIndexed { vIdx, v ->
                                val sysId = systemEntries.firstOrNull()?.id
                                val voiceSelected = selectedId == sysId && pickedSystemVoice == v.name
                                val rowShape = articulatedItemShape(
                                    index = systemEntries.size + vIdx,
                                    count = totalRows,
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (voiceSelected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = if (ChatColors.isDark) 0.42f else 0.56f,
                                                )
                                            } else cardColor,
                                            rowShape,
                                        )
                                        .clip(rowShape)
                                        .clickable {
                                            VoiceOutputState.setSystemVoice(v.name, v.label)
                                            select(sysId)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SelectionMark(selected = voiceSelected)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            v.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (voiceSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            v.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Per-Provider Articulated Cards ──
                val entriesByInstance = config.instances
                    .filter { it.isEnabled }
                    .mapNotNull { inst ->
                        val sectionEntries = ModelEntryRanking.sortedByReleaseRank(
                            config.modelEntries.filter { e ->
                                e.providerInstanceId == inst.id && !e.isHidden &&
                                    modalityFilter.matches(e.model)
                            },
                        )
                        val entries = if (matches(inst.label.ifEmpty { inst.providerType.displayName })) sectionEntries
                        else ModelSearch.rank(sectionEntries, query)
                        if (entries.isEmpty()) null else inst to entries
                    }

                entriesByInstance.forEach { (inst, entries) ->
                    item("p_${inst.id}") {
                        val collapsed = collapsedInstanceIds.contains(inst.id)
                        val dotColor = providerDotColor(inst.providerType)
                        val providerName = inst.label.ifEmpty { inst.providerType.displayName }
                        val chevronRotation by animateFloatAsState(
                            targetValue = if (collapsed) 0f else 180f,
                            label = "unifiedProviderChevron",
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.GroupItemGap),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            val headerShape = if (collapsed) {
                                RoundedCornerShape(ExpressiveTokens.OuterGroupRadius)
                            } else {
                                RoundedCornerShape(
                                    topStart = ExpressiveTokens.OuterGroupRadius,
                                    topEnd = ExpressiveTokens.OuterGroupRadius,
                                    bottomStart = ExpressiveTokens.InnerGroupRadius,
                                    bottomEnd = ExpressiveTokens.InnerGroupRadius,
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cardColor, headerShape)
                                    .clip(headerShape)
                                    .clickable {
                                        collapsedInstanceIds =
                                            if (collapsed) collapsedInstanceIds - inst.id
                                            else collapsedInstanceIds + inst.id
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
                                    providerName,
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
                                        stringResource(R.string.model_picker_groups_edit),
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
                                            .clickable { onEditProvider(inst.id) }
                                            .semantics { contentDescription = editLabel }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
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
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .graphicsLayer { rotationZ = chevronRotation },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (!collapsed) {
                                entries.forEachIndexed { i, entry ->
                                    val isLast = i == entries.size - 1
                                    val isRowSelected = selectedId == entry.id
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
                                            .fillMaxWidth()
                                            .background(
                                                if (isRowSelected) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = if (ChatColors.isDark) 0.42f else 0.56f,
                                                    )
                                                } else cardColor,
                                                rowShape,
                                            )
                                            .clip(rowShape)
                                            .clickable { select(entry.id) }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        SelectionMark(selected = isRowSelected)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                entry.model.displayName.ifBlank { entry.model.id },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isRowSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                                itemVerticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    entry.baseModel.id,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                )
                                                modalityBadges(entry.model).forEach { badge ->
                                                    ModalityBadge(badge)
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer,
                                                    RoundedCornerShape(10.dp),
                                                )
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { quickTestEntry = entry },
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
                }
            }
        }
    }

    quickTestEntry?.let { entry ->
        QuickTestSheet(
            entry = entry,
            providerRepository = providerRepository,
            onDismiss = { quickTestEntry = null },
        )
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    Icon(
        if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun systemEntrySubtitle(entry: ModelEntry): String = when (entry.model.id) {
    SystemVoiceIds.SYSTEM_TTS -> stringResource(R.string.voice_system_tts_subtitle)
    SystemVoiceIds.SYSTEM_ASR_ONLINE -> stringResource(R.string.voice_system_online_subtitle)
    SystemVoiceIds.SYSTEM_ASR_OFFLINE -> stringResource(R.string.voice_system_offline_subtitle)
    else -> ""
}
