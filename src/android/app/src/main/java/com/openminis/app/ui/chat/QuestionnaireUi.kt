package com.openminis.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openminis.app.R
import com.openminis.app.tools.AskUserTool
import com.openminis.app.ui.components.MinisButton
import com.openminis.app.ui.components.MinisTonalButton
import com.openminis.app.ui.theme.ExpressiveTokens
import com.openminis.app.ui.theme.articulatedItemShape
import kotlinx.coroutines.CompletableDeferred

/**
 * In-flight interactive questionnaire suspended inside `ChatViewModel.executeAskUserTool`.
 */
data class PendingQuestionnaire(
    val toolId: String,
    val assistantId: String,
    val spec: AskUserTool.QuestionnaireSpec,
    val deferred: CompletableDeferred<AskUserTool.QuestionnaireResult>,
)

@Stable
internal class QuestionnaireDraftState(
    val spec: AskUserTool.QuestionnaireSpec,
) {
    val selectedByQuestion = mutableStateMapOf<String, Set<String>>()
    val otherEnabledByQuestion = mutableStateMapOf<String, Boolean>()
    val customTextByQuestion = mutableStateMapOf<String, String>()

    fun toggleOption(q: AskUserTool.QuestionItem, label: String) {
        val current = selectedByQuestion[q.id].orEmpty()
        when (q.kind) {
            AskUserTool.QuestionKind.SINGLE_SELECT -> {
                selectedByQuestion[q.id] = if (label in current) emptySet() else setOf(label)
                if (label !in current) {
                    otherEnabledByQuestion[q.id] = false
                }
            }
            AskUserTool.QuestionKind.MULTI_SELECT -> {
                selectedByQuestion[q.id] = if (label in current) current - label else current + label
            }
            AskUserTool.QuestionKind.TEXT -> Unit
        }
    }

    fun toggleOther(q: AskUserTool.QuestionItem) {
        val next = !(otherEnabledByQuestion[q.id] ?: false)
        otherEnabledByQuestion[q.id] = next
        if (next && q.kind == AskUserTool.QuestionKind.SINGLE_SELECT) {
            selectedByQuestion[q.id] = emptySet()
        }
    }

    fun setCustomText(q: AskUserTool.QuestionItem, value: String) {
        customTextByQuestion[q.id] = value
        if (value.isNotBlank() && q.kind == AskUserTool.QuestionKind.SINGLE_SELECT) {
            otherEnabledByQuestion[q.id] = true
            selectedByQuestion[q.id] = emptySet()
        }
    }

    val hasAnyAnswer: Boolean
        get() = spec.questions.any { q ->
            val sel = selectedByQuestion[q.id].orEmpty()
            val txt = customTextByQuestion[q.id]?.trim().orEmpty()
            val otherOn = otherEnabledByQuestion[q.id] == true || q.kind == AskUserTool.QuestionKind.TEXT
            sel.isNotEmpty() || (otherOn && txt.isNotEmpty())
        }

    fun buildResult(): AskUserTool.QuestionnaireResult {
        val answers = spec.questions.map { q ->
            val sel = selectedByQuestion[q.id].orEmpty().toList()
            val otherOn = otherEnabledByQuestion[q.id] == true || q.kind == AskUserTool.QuestionKind.TEXT
            val txt = if (otherOn) customTextByQuestion[q.id]?.trim()?.takeIf { it.isNotEmpty() } else null
            AskUserTool.QuestionAnswer(
                id = q.id,
                question = q.question,
                header = q.header,
                selected = sel,
                customText = txt,
            )
        }
        return AskUserTool.QuestionnaireResult(
            status = "answered",
            answers = answers,
        )
    }
}

/**
 * Material 3 Expressive inline Questionnaire Card rendered in the chat stream
 * when `block.toolName == AskUserTool.NAME`.
 */
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)
@Composable
internal fun AskUserToolCard(
    block: AssistantBlock,
    pending: PendingQuestionnaire?,
    onSubmit: (String, AskUserTool.QuestionnaireResult) -> Unit,
    onSkip: (String) -> Unit,
    onOpenDetail: (AssistantBlock) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLiveInteractive = pending != null && pending.toolId == block.id
    val parsedSpec = remember(block.toolArgs, pending?.toolId) {
        if (isLiveInteractive) pending?.spec else AskUserTool.parseSpec(block.toolArgs)
    }
    val parsedResult = remember(block.content) {
        AskUserTool.parseResult(block.content)
    }

    val draftState = remember(block.id, parsedSpec) {
        parsedSpec?.let { QuestionnaireDraftState(it) }
    }
    var showFullSheet by remember(block.id) { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(26.dp)
    val borderColor = when {
        isLiveInteractive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
        block.toolStatus == ToolBlockStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }
    val containerColor = if (isLiveInteractive) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Surface(
        shape = cardShape,
        color = containerColor,
        tonalElevation = if (isLiveInteractive) 2.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = if (isLiveInteractive) 1.5.dp else 1.dp,
                color = borderColor,
                shape = cardShape,
            )
            .clip(cardShape)
            .then(
                if (!isLiveInteractive) {
                    Modifier.clickable { onOpenDetail(block) }
                } else {
                    Modifier
                },
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Expressive Header Band
            val headerBg = if (isLiveInteractive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.65f)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val badgeShape = MaterialShapes.Sunny.toShape()
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = if (isLiveInteractive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            shape = badgeShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChecklistRtl,
                        contentDescription = null,
                        tint = if (isLiveInteractive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.size(19.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    val displayTitle = parsedSpec?.toolTitle?.takeIf { it.isNotBlank() }
                        ?: block.toolTitle.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.agent_tools_ask_user)
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val statusText = when {
                        isLiveInteractive -> stringResource(R.string.questionnaire_status_waiting)
                        parsedResult?.status == "skipped" -> stringResource(R.string.questionnaire_status_skipped)
                        parsedResult?.status == "cancelled" || block.toolStatus == ToolBlockStatus.FAILED ->
                            stringResource(R.string.questionnaire_status_cancelled)
                        else -> stringResource(R.string.questionnaire_status_answered)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLiveInteractive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                if (isLiveInteractive && draftState != null) {
                    IconButton(
                        onClick = { showFullSheet = true },
                        modifier = Modifier.size(34.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = stringResource(R.string.questionnaire_expand_sheet),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (isLiveInteractive && parsedSpec != null && draftState != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.GroupItemGap),
                ) {
                    parsedSpec.questions.forEachIndexed { idx, q ->
                        QuestionInteractiveItem(
                            question = q,
                            index = idx,
                            totalCount = parsedSpec.questions.size,
                            draftState = draftState,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MinisTonalButton(
                            onClick = { onSkip(block.id) },
                        ) {
                            Text(
                                text = stringResource(R.string.questionnaire_skip),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        MinisButton(
                            onClick = { onSubmit(block.id, draftState.buildResult()) },
                            enabled = draftState.hasAnyAnswer,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.questionnaire_submit),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            } else {
                // Completed / Skipped / Read-only compact summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val answers = parsedResult?.answers.orEmpty()
                    if (answers.isNotEmpty()) {
                        answers.forEach { ans ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    if (!ans.header.isNullOrBlank()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                        ) {
                                            Text(
                                                text = ans.header,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = ans.question,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                val summary = ans.displaySummary
                                if (summary.isNotBlank()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        val chips = buildList {
                                            addAll(ans.selected.filter { it.isNotBlank() })
                                            ans.customText?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                                        }
                                        chips.forEach { chipText ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(13.dp),
                                                    )
                                                    Text(
                                                        text = chipText,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (parsedSpec != null) {
                        parsedSpec.questions.take(3).forEach { q ->
                            Text(
                                text = "• ${q.question}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFullSheet && isLiveInteractive && parsedSpec != null && draftState != null) {
        QuestionnaireModalSheet(
            spec = parsedSpec,
            draftState = draftState,
            onDismiss = { showFullSheet = false },
            onSkip = {
                showFullSheet = false
                onSkip(block.id)
            },
            onSubmit = {
                showFullSheet = false
                onSubmit(block.id, draftState.buildResult())
            },
        )
    }
}

@Composable
private fun QuestionInteractiveItem(
    question: AskUserTool.QuestionItem,
    index: Int,
    totalCount: Int,
    draftState: QuestionnaireDraftState,
) {
    val itemShape = articulatedItemShape(index = index, count = totalCount)
    val selectedSet = draftState.selectedByQuestion[question.id].orEmpty()
    val otherEnabled = draftState.otherEnabledByQuestion[question.id] == true
    val customText = draftState.customTextByQuestion[question.id].orEmpty()

    Surface(
        shape = itemShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Question Header + Text
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (totalCount > 1) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "${index + 1}/$totalCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (!question.header.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = question.header,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (question.kind == AskUserTool.QuestionKind.MULTI_SELECT) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.questionnaire_multi_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Text(
                    text = question.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Selectable Options
            if (question.kind != AskUserTool.QuestionKind.TEXT && question.options.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    question.options.forEach { option ->
                        val isSelected = option.label in selectedSet
                        OptionChoiceRow(
                            label = option.label,
                            description = option.description,
                            isSelected = isSelected,
                            isMulti = question.kind == AskUserTool.QuestionKind.MULTI_SELECT,
                            onClick = { draftState.toggleOption(question, option.label) },
                        )
                    }

                    if (question.allowOther) {
                        OptionChoiceRow(
                            label = stringResource(R.string.questionnaire_other_label),
                            description = null,
                            isSelected = otherEnabled,
                            isMulti = question.kind == AskUserTool.QuestionKind.MULTI_SELECT,
                            isOther = true,
                            onClick = { draftState.toggleOther(question) },
                        )
                        AnimatedVisibility(visible = otherEnabled) {
                            OutlinedTextField(
                                value = customText,
                                onValueChange = { draftState.setCustomText(question, it) },
                                placeholder = {
                                    Text(
                                        question.placeholder
                                            ?: stringResource(R.string.questionnaire_other_placeholder),
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                ),
                                minLines = 1,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            )
                        }
                    }
                }
            } else {
                // Free-text input
                OutlinedTextField(
                    value = customText,
                    onValueChange = { draftState.setCustomText(question, it) },
                    placeholder = {
                        Text(
                            question.placeholder
                                ?: stringResource(R.string.questionnaire_text_placeholder),
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OptionChoiceRow(
    label: String,
    description: String?,
    isSelected: Boolean,
    isMulti: Boolean,
    isOther: Boolean = false,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        animationSpec = tween(160),
        label = "optBg",
    )
    val borderCol by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        },
        animationSpec = tween(160),
        label = "optBorder",
    )
    val shape = RoundedCornerShape(if (isSelected) 16.dp else 12.dp)

    Surface(
        shape = shape,
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderCol,
                shape = shape,
            )
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val iconVec = when {
                isOther && !isSelected -> Icons.Default.Edit
                isMulti && isSelected -> Icons.Default.CheckBox
                isMulti && !isSelected -> Icons.Default.CheckBoxOutlineBlank
                isSelected -> Icons.Default.CheckCircle
                else -> Icons.Outlined.Circle
            }
            Icon(
                imageVector = iconVec,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuestionnaireModalSheet(
    spec: AskUserTool.QuestionnaireSpec,
    draftState: QuestionnaireDraftState,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onSubmit: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = ExpressiveTokens.SheetTopRadius,
            topEnd = ExpressiveTokens.SheetTopRadius,
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            MaterialShapes.Sunny.toShape(),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.ChecklistRtl,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = spec.toolTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.GroupItemGap),
            ) {
                spec.questions.forEachIndexed { idx, q ->
                    QuestionInteractiveItem(
                        question = q,
                        index = idx,
                        totalCount = spec.questions.size,
                        draftState = draftState,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                MinisTonalButton(onClick = onSkip) {
                    Text(stringResource(R.string.questionnaire_skip))
                }
                MinisButton(
                    onClick = onSubmit,
                    enabled = draftState.hasAnyAnswer,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.questionnaire_submit))
                }
            }
        }
    }
}

/**
 * Rich Q&A breakdown rendered inside `ToolDetailSheet` when `block.toolName == "ask_user"`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuestionnaireDetailContent(block: AssistantBlock) {
    val spec = remember(block.toolArgs) { AskUserTool.parseSpec(block.toolArgs) }
    val result = remember(block.content) { AskUserTool.parseResult(block.content) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val statusLabel = when (result?.status) {
            "answered" -> stringResource(R.string.questionnaire_status_answered)
            "skipped" -> stringResource(R.string.questionnaire_status_skipped)
            "cancelled" -> stringResource(R.string.questionnaire_status_cancelled)
            else -> stringResource(R.string.questionnaire_status_waiting)
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }

        val questions = spec?.questions.orEmpty()
        val answerMap = result?.answers.orEmpty().associateBy { it.id }

        if (questions.isNotEmpty()) {
            questions.forEachIndexed { idx, q ->
                val ans = answerMap[q.id]
                    ?: result?.answers?.getOrNull(idx)
                Surface(
                    shape = articulatedItemShape(idx, questions.size),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!q.header.isNullOrBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                            ) {
                                Text(
                                    text = q.header,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = q.question,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (q.options.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                q.options.forEach { opt ->
                                    val chosen = ans?.selected?.contains(opt.label) == true
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (chosen) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                            contentDescription = null,
                                            tint = if (chosen) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            },
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = opt.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (chosen) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        if (!ans?.customText.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = ans?.customText.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                    }
                }
            }
        } else if (block.content.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = block.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }
}
