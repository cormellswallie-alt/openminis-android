package com.openminis.app.ui.scheduled

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import com.openminis.app.ui.theme.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import com.openminis.app.ui.theme.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openminis.app.R
import com.openminis.app.scheduled.ScheduledRepeatMode
import com.openminis.app.scheduled.ScheduledTask
import com.openminis.app.ui.components.MinisAlertDialog
import com.openminis.app.ui.components.MinisButton
import com.openminis.app.ui.components.MinisMenu
import com.openminis.app.ui.settings.SettingsSwitch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledTasksScreen(
    onBack: () -> Unit,
    onEditTask: (taskId: String?) -> Unit,
    onViewRuns: (taskId: String) -> Unit,
    onOpenSession: (sessionId: String) -> Unit,
) {
    val context = LocalContext.current
    val vm: ScheduledTasksViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ScheduledTasksViewModel.factory(context),
    )
    val tasks by vm.tasks.collectAsState()
    var pendingDelete by remember { mutableStateOf<ScheduledTask?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.scheduled_tasks_title),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEditTask(null) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.scheduled_task_new),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            EmptyState(padding = padding, onCreate = { onEditTask(null) })
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(tasks, key = { it.id }) { task ->
                ScheduledTaskRow(
                    task = task,
                    onClick = { onEditTask(task.id) },
                    onToggle = { vm.setEnabled(task.id, it) },
                    onEdit = { onEditTask(task.id) },
                    onViewRuns = { onViewRuns(task.id) },
                    onDelete = { pendingDelete = task },
                )
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        MinisAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.scheduled_task_delete_title),
            text = stringResource(R.string.scheduled_task_delete_body, toDelete.label),
            confirmText = stringResource(R.string.scheduled_task_delete_confirm),
            isDestructive = true,
            onConfirm = {
                vm.delete(toDelete.id)
                pendingDelete = null
            },
            dismissText = stringResource(R.string.cancel),
        )
    }
}

@Composable
private fun EmptyState(
    padding: PaddingValues,
    onCreate: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f), MaterialShapes.Cookie12Sided.toShape()),
                )
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialShapes.Sunny.toShape()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                stringResource(R.string.scheduled_tasks_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            MinisButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.scheduled_task_new))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduledTaskRow(
    task: ScheduledTask,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onViewRuns: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember(task.id) { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true }),
            shape = RoundedCornerShape(24.dp),
            color = if (task.enabled) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val badgeShape = if (task.enabled) {
                    MaterialShapes.Cookie9Sided.toShape()
                } else {
                    RoundedCornerShape(14.dp)
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(badgeShape)
                        .background(
                            if (task.enabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = if (task.enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = task.label.ifBlank { task.prompt.take(40) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (task.enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (task.enabled) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ) {
                        Text(
                            text = formatScheduleSummary(task),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (task.enabled) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                SettingsSwitch(checked = task.enabled, onCheckedChange = onToggle)
            }
        }

        MinisMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.scheduled_task_menu_edit)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = { menuExpanded = false; onEdit() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.scheduled_task_menu_runs)) },
                leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) },
                onClick = { menuExpanded = false; onViewRuns() },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.scheduled_task_menu_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = { menuExpanded = false; onDelete() },
            )
        }
    }
}

internal fun formatScheduleSummary(task: ScheduledTask): String {
    val nextSuffix = task.nextTriggerMs()?.let {
        " · next ${SimpleDateFormat("MMM d HH:mm", Locale.getDefault()).format(Date(it))}"
    } ?: ""
    when (task.triggerKind) {
        com.openminis.app.scheduled.ScheduledTriggerKind.AFTER ->
            return "Once after ${com.openminis.app.scheduled.ScheduledTaskDetailModel.formatDuration(task.delaySec ?: 0L)}$nextSuffix"
        com.openminis.app.scheduled.ScheduledTriggerKind.INTERVAL -> {
            val every = com.openminis.app.scheduled.ScheduledTaskDetailModel.formatDuration(task.intervalSec ?: 0L)
            val count = task.maxFires?.let { " ×$it (${task.remainingFires} left)" } ?: ""
            return "Every $every$count$nextSuffix"
        }
        com.openminis.app.scheduled.ScheduledTriggerKind.ON_COMPLETION ->
            return "After ${task.onCompletionOf.orEmpty().take(8)} finishes"
        com.openminis.app.scheduled.ScheduledTriggerKind.CALENDAR -> Unit
    }
    val time = "%02d:%02d".format(task.timeOfDayHour, task.timeOfDayMinute)
    val repeat = when (task.repeatMode) {
        ScheduledRepeatMode.ONCE -> "Once"
        ScheduledRepeatMode.DAILY -> "Daily"
        ScheduledRepeatMode.WEEKDAYS -> "Weekdays"
        ScheduledRepeatMode.CUSTOM -> {
            val days = task.customDays.sorted().joinToString(",") { dowShort(it) }
            if (days.isBlank()) "Custom" else days
        }
    }
    val next = task.nextTriggerMs()?.let {
        val sdf = SimpleDateFormat("MMM d HH:mm", Locale.getDefault())
        " · next ${sdf.format(Date(it))}"
    } ?: ""
    return "$repeat $time$next"
}

private fun dowShort(dow: Int): String = when (dow) {
    java.util.Calendar.SUNDAY -> "Sun"
    java.util.Calendar.MONDAY -> "Mon"
    java.util.Calendar.TUESDAY -> "Tue"
    java.util.Calendar.WEDNESDAY -> "Wed"
    java.util.Calendar.THURSDAY -> "Thu"
    java.util.Calendar.FRIDAY -> "Fri"
    java.util.Calendar.SATURDAY -> "Sat"
    else -> ""
}
