package com.openminis.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.data.model.ModelGroup
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.ui.components.ExpressiveArticulatedSeam
import com.openminis.app.ui.components.MinisButton
import com.openminis.app.ui.components.MinisMenu
import com.openminis.app.ui.components.MinisMenuDefaults
import com.openminis.app.ui.components.MinisTextButton
import com.openminis.app.ui.components.MinisTonalButton
import com.openminis.app.ui.settings.ExpressiveNavIconButton
import com.openminis.app.ui.theme.ExpressiveLoadingIndicator
import com.openminis.app.ui.theme.ExpressiveTokens
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.toShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Onboarding step 2: pick 1-3 models from configured providers
 * and create a "Default Models" group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingModelSelectionScreen(
    providerRepository: ProviderRepository,
    onBack: () -> Unit,
    onAddCustomModel: (instanceId: String) -> Unit = {},
) {
    val config by providerRepository.config.collectAsState()
    val selected = remember { mutableStateListOf<String>() }
    var searchText by remember { mutableStateOf("") }

    val enabledInstances = config.instances.filter { it.isEnabled }
    val enabledInstanceIds = enabledInstances.map { it.id }.toSet()
    val allEntries = config.modelEntries.filter {
        it.providerInstanceId in enabledInstanceIds && !it.isHidden
    }

    var fetching by remember { mutableStateOf(false) }
    var fetchAttempted by rememberSaveable { mutableStateOf(false) }
    var failureDetail by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val timeoutText = stringResource(
        R.string.onboarding_models_fetch_timeout,
        (OnboardingModelFetch.TIMEOUT_MS / 1000).toInt(),
    )

    fun visibleEntryCount(instanceIds: Set<String>): Int =
        providerRepository.config.value.modelEntries.count { it.providerInstanceId in instanceIds && !it.isHidden }

    fun runBoundedFetch() {
        if (fetching) return
        val current = providerRepository.config.value
        val targets = current.instances.filter { inst ->
            inst.isEnabled && current.modelEntries.none { it.providerInstanceId == inst.id && !it.isHidden }
        }
        fetchAttempted = true
        failureDetail = null
        if (targets.isEmpty()) {
            if (visibleEntryCount(enabledInstanceIds) == 0) failureDetail = ""
            return
        }
        fetching = true
        scope.launch {
            try {
                val result = OnboardingModelFetch.run(targets) { inst ->
                    var vendorError: String? = null
                    withContext(Dispatchers.IO) {
                        providerRepository.refreshModels(inst, onVendorError = { vendorError = it })
                    }
                    vendorError
                }
                val ids = providerRepository.config.value.instances.filter { it.isEnabled }.map { it.id }.toSet()
                if (visibleEntryCount(ids) == 0) {
                    failureDetail = if (result.timedOut) timeoutText else result.errors.distinct().joinToString("\n")
                }
            } finally {
                fetching = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (fetchAttempted) return@LaunchedEffect
        if (allEntries.isEmpty()) {
            runBoundedFetch()
            return@LaunchedEffect
        }
        for (instance in enabledInstances) {
            providerRepository.triggerAsyncModelReconcile(instance.id)
        }
    }

    val filteredEntries = if (searchText.isBlank()) allEntries else {
        val q = searchText.lowercase()
        allEntries.filter {
            it.model.displayName.lowercase().contains(q) || it.model.id.lowercase().contains(q)
        }
    }
    val grouped = filteredEntries.groupBy { it.providerInstanceId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.onboarding_select_models_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    ExpressiveNavIconButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.common_back),
                    )
                },
                actions = {
                    MinisTextButton(onClick = onBack) {
                        Text(stringResource(R.string.common_skip))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                stringResource(R.string.onboarding_select_models_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text(stringResource(R.string.onboarding_filter_models)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )

            Spacer(Modifier.height(10.dp))

            if (allEntries.isEmpty() && !fetching && failureDetail != null) {
                ModelFetchFailure(
                    detail = failureDetail.orEmpty(),
                    instances = enabledInstances.map { it.id to it.label },
                    onRetry = { runBoundedFetch() },
                    onAddCustomModel = onAddCustomModel,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else if (allEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ExpressiveLoadingIndicator(modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.onboarding_loading_models),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    for ((instanceId, entries) in grouped) {
                        val instance = config.instances.find { it.id == instanceId }
                        item(key = "header_$instanceId") {
                            Column {
                                Text(
                                    instance?.label ?: instanceId,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(ExpressiveTokens.OuterGroupRadius))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                ) {
                                    entries.forEachIndexed { idx, entry ->
                                        val isSelected = entry.id in selected
                                        val selectionIndex = selected.indexOf(entry.id)
                                        val canSelect = selected.size < 3 || isSelected

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isSelected) {
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceContainerLow
                                                    },
                                                )
                                                .clickable(enabled = canSelect) {
                                                    if (isSelected) selected.remove(entry.id)
                                                    else if (selected.size < 3) selected.add(entry.id)
                                                }
                                                .padding(vertical = 12.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(
                                                        color = if (isSelected) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                                        },
                                                        shape = if (isSelected) {
                                                            MaterialShapes.SoftBurst.toShape()
                                                        } else {
                                                            CircleShape
                                                        },
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (isSelected) {
                                                    Text(
                                                        "${selectionIndex + 1}",
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    entry.model.displayName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                )
                                                Text(
                                                    entry.model.id,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                        if (idx < entries.lastIndex) {
                                            ExpressiveArticulatedSeam()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            MinisButton(
                onClick = {
                    if (selected.isNotEmpty()) {
                        val group = ModelGroup(name = "Default Models")
                        group.memberEntryIds.addAll(selected)
                        providerRepository.addGroup(group)
                        if (config.defaultPrimaryGroupId == null) {
                            providerRepository.defaultPrimaryGroupId = group.id
                        }
                    }
                    onBack()
                },
                enabled = selected.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    stringResource(R.string.onboarding_done_count, selected.size),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ModelFetchFailure(
    detail: String,
    instances: List<Pair<String, String>>,
    onRetry: () -> Unit,
    onAddCustomModel: (instanceId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(ExpressiveTokens.OuterGroupRadius),
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialShapes.SoftBurst.toShape(),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            val headline = stringResource(R.string.onboarding_models_fetch_failed)
            Text(
                headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (detail.isNotBlank() && detail != headline) {
                Spacer(Modifier.height(6.dp))
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 6,
                )
            }
            Spacer(Modifier.height(18.dp))
            MinisButton(onClick = onRetry) {
                Text(stringResource(R.string.onboarding_models_retry))
            }
            if (instances.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Box {
                    MinisTonalButton(onClick = {
                        if (instances.size == 1) onAddCustomModel(instances.first().first) else pickerOpen = true
                    }) {
                        Text(stringResource(R.string.onboarding_models_add_manually))
                    }
                    MinisMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                        for ((id, label) in instances) {
                            DropdownMenuItem(
                                text = { Text(label.ifBlank { id }) },
                                colors = MinisMenuDefaults.itemColors(),
                                onClick = {
                                    pickerOpen = false
                                    onAddCustomModel(id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
