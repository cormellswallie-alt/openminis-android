package com.openminis.app.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.browser.BrowserHistoryStore
import com.openminis.app.ui.components.ExpressiveArticulatedSeam
import com.openminis.app.ui.components.MinisAlertDialog
import com.openminis.app.ui.components.MinisSmallButton
import com.openminis.app.ui.components.MinisSmallOutlinedButton
import com.openminis.app.ui.settings.ExpressiveEmptyStateCard
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.toShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserHistorySheet(
    historyStore: BrowserHistoryStore,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val entries = remember(searchQuery, historyStore.getEntries().size) {
        if (searchQuery.isBlank()) historyStore.groupedByDay()
        else mapOf("Results" to historyStore.search(searchQuery))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.browser_history_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (historyStore.getEntries().isNotEmpty()) {
                    MinisSmallOutlinedButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            stringResource(R.string.browser_history_clear),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                MinisSmallButton(onClick = onDismiss) {
                    Text(stringResource(R.string.browser_history_done), fontWeight = FontWeight.SemiBold)
                }
            }

            // Search bar (Expressive capsule)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.browser_history_search)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                singleLine = true,
                shape = CircleShape,
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(6.dp))

            if (entries.values.flatten().isEmpty()) {
                ExpressiveEmptyStateCard(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.browser_history_empty_title),
                    subtitle = stringResource(R.string.browser_history_empty_subtitle),
                    badgeShape = MaterialShapes.Cookie9Sided.toShape(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp),
                ) {
                    for ((dayLabel, dayEntries) in entries) {
                        if (dayEntries.isEmpty()) continue
                        item(key = "section_$dayLabel") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    dayLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 6.dp),
                                )
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                ) {
                                    dayEntries.forEachIndexed { index, entry ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onNavigate(entry.url) }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.secondaryContainer,
                                                        RoundedCornerShape(11.dp),
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    Icons.Default.Language,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    entry.title.ifEmpty { entry.domain },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    entry.url,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Text(
                                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp)),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        if (index < dayEntries.size - 1) {
                                            ExpressiveArticulatedSeam()
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

    if (showClearConfirm) {
        MinisAlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = stringResource(R.string.browser_history_clear_dialog_title),
            text = stringResource(R.string.browser_history_clear_dialog_message),
            confirmText = stringResource(R.string.browser_history_clear),
            isDestructive = true,
            onConfirm = {
                historyStore.clear()
                showClearConfirm = false
            },
        )
    }
}
