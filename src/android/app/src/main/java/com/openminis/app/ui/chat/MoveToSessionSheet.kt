package com.openminis.app.ui.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.data.db.ChatSessionEntity
import com.openminis.app.data.repository.ChatRepository
import com.openminis.app.ui.components.ExpressiveArticulatedSeam
import com.openminis.app.ui.settings.ExpressiveEmptyStateCard
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.toShape
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val MOVE_TO_SESSION_LIMIT = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToSessionSheet(
    currentSessionId: String,
    chatRepository: ChatRepository,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    initialSessions: List<ChatSessionEntity> = emptyList(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val seeded by remember(currentSessionId) {
        chatRepository.dao.observeSessions()
            .map { all ->
                all.asSequence()
                    .filter { it.id != currentSessionId && !it.isChild }
                    .take(MOVE_TO_SESSION_LIMIT)
                    .toList()
            }
    }.collectAsState(initial = initialSessions)

    var refreshed by remember { mutableStateOf<List<ChatSessionEntity>?>(null) }
    val sessions = refreshed ?: seeded

    LaunchedEffect(currentSessionId) {
        refreshed = withContext(Dispatchers.IO) {
            chatRepository.dao.listRecentMovableSessions(
                excludeId = currentSessionId,
                limit = MOVE_TO_SESSION_LIMIT,
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            StandardChatSheetHeader(
                title = stringResource(R.string.move_to_sheet_title),
                onDismiss = onDismiss,
            )
            Spacer(Modifier.height(10.dp))
            NewChatRow(
                onClick = { onSelect("__new__${UUID.randomUUID()}") },
            )
            if (sessions.isEmpty()) {
                ExpressiveEmptyStateCard(
                    icon = Icons.Outlined.Forum,
                    title = stringResource(R.string.move_to_sheet_empty),
                    badgeShape = MaterialShapes.SoftBurst.toShape(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(color = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    itemsIndexed(sessions, key = { _, it -> it.id }) { index, session ->
                        MoveToPickerRow(
                            session = session,
                            onClick = { onSelect(session.id) },
                        )
                        if (index < sessions.size - 1) {
                            ExpressiveArticulatedSeam()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewChatRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialShapes.Sunny.toShape(),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = stringResource(R.string.new_chat),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(Modifier.size(10.dp))
}

@Composable
private fun MoveToPickerRow(
    session: ChatSessionEntity,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val style = remember(session.category) { categoryStyle(session.category) }
    val timeText = remember(session.updatedAt, context) { relativeDate(context, session.updatedAt) }
    val untitled = stringResource(R.string.move_to_sheet_untitled)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color = style.color.copy(alpha = 0.16f), shape = RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.color,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = session.title ?: untitled,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private data class CategoryStyle(val icon: ImageVector, val color: Color)

private fun categoryStyle(category: String?): CategoryStyle {
    return when (category?.lowercase()) {
        "code"         -> CategoryStyle(Icons.Outlined.Code, Color(0xFFD97706))
        "writing"      -> CategoryStyle(Icons.Outlined.Description, Color(0xFF2563EB))
        "research"     -> CategoryStyle(Icons.Outlined.Language, Color(0xFF0891B2))
        "analysis"     -> CategoryStyle(Icons.Outlined.BarChart, Color(0xFF4F46E5))
        "creative"     -> CategoryStyle(Icons.Outlined.Brush, Color(0xFFEC4899))
        "chat"         -> CategoryStyle(Icons.Outlined.Forum, Color(0xFF10B981))
        "math"         -> CategoryStyle(Icons.Outlined.Calculate, Color(0xFF9333EA))
        "translation"  -> CategoryStyle(Icons.Outlined.Translate, Color(0xFF06B6D4))
        "health"       -> CategoryStyle(Icons.Outlined.Favorite, Color(0xFFEF4444))
        "finance"      -> CategoryStyle(Icons.Outlined.Payments, Color(0xFF0D9488))
        "travel"       -> CategoryStyle(Icons.Outlined.Map, Color(0xFFD97706))
        "education"    -> CategoryStyle(Icons.Outlined.Book, Color(0xFF2563EB))
        "design"       -> CategoryStyle(Icons.Outlined.Palette, Color(0xFFEC4899))
        "productivity" -> CategoryStyle(Icons.Outlined.CalendarMonth, Color(0xFFEAB308))
        "support"      -> CategoryStyle(Icons.Outlined.Settings, Color(0xFF8B6914))
        "other"        -> CategoryStyle(Icons.Outlined.GridView, Color(0xFF737785))
        else           -> CategoryStyle(Icons.Outlined.Forum, Color(0xFF737785))
    }
}

private fun relativeDate(context: Context, timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    if (seconds < 60) return context.getString(R.string.time_just_now)
    if (minutes < 60) return context.getString(R.string.time_minutes_ago, minutes.toInt())
    if (hours < 24) return context.getString(R.string.time_hours_ago, hours.toInt())
    val dateCal = Calendar.getInstance().apply { time = Date(timestamp) }
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (dateCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
        dateCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)
    ) return context.getString(R.string.time_yesterday)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    if (days < 7) {
        val dayNames = java.text.DateFormatSymbols(java.util.Locale.getDefault()).weekdays
        return dayNames[dateCal.get(Calendar.DAY_OF_WEEK) - 1]
    }
    val month = dateCal.get(Calendar.MONTH) + 1
    val day = dateCal.get(Calendar.DAY_OF_MONTH)
    return "$month/$day"
}
