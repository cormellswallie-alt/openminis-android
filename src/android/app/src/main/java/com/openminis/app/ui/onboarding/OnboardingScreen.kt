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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.model.ModelGroup
import com.openminis.app.data.model.ProviderCredential
import com.openminis.app.data.model.ProviderInstance
import com.openminis.app.data.model.ProviderType
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.ui.components.ExpressiveArticulatedSeam
import com.openminis.app.ui.components.MinisButton
import com.openminis.app.ui.components.MinisTonalButton
import com.openminis.app.ui.theme.ExpressiveTokens
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.toShape

/**
 * Material 3 Expressive multi-step onboarding flow shown on first launch.
 */
@Composable
fun OnboardingScreen(
    providerRepository: ProviderRepository,
    onComplete: () -> Unit,
) {
    var step by remember { mutableStateOf(0) }

    when (step) {
        0 -> WelcomeStep(onNext = { step = 1 })
        1 -> ApiKeyStep(providerRepository = providerRepository, onNext = { step = 2 }, onSkip = onComplete)
        2 -> ModelSelectionStep(providerRepository = providerRepository, onComplete = onComplete)
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialShapes.Sunny.toShape(),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.onboarding_welcome_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Text(
                        stringResource(R.string.onboarding_welcome_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
                Spacer(Modifier.height(44.dp))
                MinisButton(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(0.75f),
                ) {
                    Text(
                        stringResource(R.string.onboarding_get_started),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyStep(
    providerRepository: ProviderRepository,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val providerOptions = listOf(
        ProviderType.anthropic,
        ProviderType.gemini,
        ProviderType.openAI,
        ProviderType.openRouter,
    )
    var selectedType by remember { mutableStateOf(ProviderType.anthropic) }
    var apiKey by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialShapes.Cookie9Sided.toShape(),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column {
                    Text(
                        stringResource(R.string.onboarding_provider_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.onboarding_provider_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Material 3 Expressive connected segmented provider selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                providerOptions.forEachIndexed { index, type ->
                    val isSelected = type == selectedType
                    val segShape = when {
                        isSelected -> CircleShape
                        index == 0 -> RoundedCornerShape(
                            topStart = 22.dp,
                            bottomStart = 22.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp,
                        )
                        index == providerOptions.lastIndex -> RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 8.dp,
                            topEnd = 22.dp,
                            bottomEnd = 22.dp,
                        )
                        else -> RoundedCornerShape(8.dp)
                    }
                    Surface(
                        onClick = {
                            selectedType = type
                            apiKey = ""
                            saved = false
                        },
                        shape = segShape,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                            Text(
                                type.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(ExpressiveTokens.OuterGroupRadius),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            saved = false
                        },
                        label = {
                            Text(
                                stringResource(
                                    R.string.onboarding_provider_api_key_label,
                                    selectedType.displayName,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )

                    Spacer(Modifier.height(14.dp))

                    MinisButton(
                        onClick = {
                            if (apiKey.isNotBlank()) {
                                val instance = ProviderInstance(
                                    id = "${selectedType.name}_onboarding",
                                    label = selectedType.displayName,
                                    providerType = selectedType,
                                    credentialType = ProviderCredential.apiKey,
                                )
                                providerRepository.addInstance(instance)
                                providerRepository.saveApiKey(instance.id, apiKey.trim())
                                for (model in selectedType.builtInModels) {
                                    providerRepository.addEntry(
                                        ModelEntry(providerInstanceId = instance.id, baseModel = model),
                                    )
                                }
                                saved = true
                            }
                        },
                        enabled = apiKey.isNotBlank() && !saved,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (saved) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            stringResource(
                                if (saved) R.string.onboarding_saved else R.string.onboarding_save_api_key,
                            ),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MinisTonalButton(onClick = onSkip) {
                    Text(stringResource(R.string.common_skip))
                }
                MinisButton(onClick = onNext, enabled = saved) {
                    Text(stringResource(R.string.common_next), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ModelSelectionStep(
    providerRepository: ProviderRepository,
    onComplete: () -> Unit,
) {
    val config by providerRepository.config.collectAsState()
    val selected = remember { mutableStateListOf<String>() }
    var searchText by remember { mutableStateOf("") }

    val enabledInstanceIds = config.instances.filter { it.isEnabled }.map { it.id }.toSet()
    val allEntries = config.modelEntries.filter {
        it.providerInstanceId in enabledInstanceIds && !it.isHidden
    }
    val filteredEntries = if (searchText.isBlank()) allEntries else {
        val q = searchText.lowercase()
        allEntries.filter {
            it.model.displayName.lowercase().contains(q) || it.model.id.lowercase().contains(q)
        }
    }
    val grouped = filteredEntries.groupBy { it.providerInstanceId }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.onboarding_select_models_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.onboarding_select_models_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text(stringResource(R.string.onboarding_search_models)) },
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

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                for ((instanceId, entries) in grouped) {
                    val instance = config.instances.find { it.id == instanceId }
                    item(key = "group_$instanceId") {
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
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MinisTonalButton(onClick = onComplete) {
                    Text(stringResource(R.string.common_skip))
                }
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
                        onComplete()
                    },
                    enabled = selected.isNotEmpty(),
                ) {
                    Text(
                        stringResource(R.string.onboarding_done_count, selected.size),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
