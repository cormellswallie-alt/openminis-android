package com.openminis.app.ui.chat

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.scheduled.ScheduledTaskDetailModel
import com.openminis.app.scheduled.ScheduledTaskManager
import com.openminis.app.scheduled.ScheduledTaskMarker
import com.openminis.app.ui.components.MinisAlertDialog
import com.openminis.app.ui.components.MinisSmallOutlinedButton
import com.openminis.app.ui.theme.ChatColors
import com.openminis.app.ui.theme.MaterialShapes
import com.openminis.app.ui.theme.toShape
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

/** [T-android-scheduled-task-card] Warm amber accent — a timer, not an agent. */
internal val ScheduledAccent = Color(0xFFD97706)

/**
 * Material 3 Expressive card for a fired scheduled task inside the chat stream.
 */
@Composable
internal fun ScheduledTaskCard(
    marker: ScheduledTaskMarker,
    modifier: Modifier = Modifier,
    hostSessionId: String? = null,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(20.dp)
    var confirming by remember { mutableStateOf(false) }
    var cancelled by remember(marker.taskId) { mutableStateOf(false) }
    var showingDetail by remember { mutableStateOf(false) }

    val now by produceState(System.currentTimeMillis(), marker.nextFireAtMs) {
        val next = marker.nextFireAtMs ?: return@produceState
        val wait = next - System.currentTimeMillis()
        if (wait > 0) {
            delay(wait + 1)
            value = System.currentTimeMillis()
        }
    }
    val nextLine: String? = when (
        val sub = ScheduledTaskDetailModel.cardSubtitle(marker, cancelled, now)
    ) {
        ScheduledTaskDetailModel.CardSubtitle.Cancelled -> stringResource(R.string.scheduled_card_cancelled)
        is ScheduledTaskDetailModel.CardSubtitle.Next -> stringResource(
            R.string.scheduled_card_next_fire,
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(sub.atMs)),
        )
        ScheduledTaskDetailModel.CardSubtitle.LastRun -> stringResource(R.string.scheduled_card_last_run)
        ScheduledTaskDetailModel.CardSubtitle.None -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, ScheduledAccent.copy(alpha = 0.28f), shape)
            .clickable { showingDetail = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(ScheduledAccent.copy(alpha = 0.16f), MaterialShapes.Sunny.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = ScheduledAccent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.scheduled_card_label),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScheduledAccent,
                    maxLines = 1,
                    modifier = Modifier
                        .background(ScheduledAccent.copy(alpha = 0.14f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Text(
                    marker.label.ifEmpty { marker.prompt.take(40) },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChatColors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (nextLine != null) {
                Text(
                    nextLine,
                    fontSize = 11.sp,
                    color = ChatColors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!cancelled) {
            Spacer(Modifier.width(8.dp))
            MinisSmallOutlinedButton(
                onClick = { confirming = true },
                border = BorderStroke(1.dp, ScheduledAccent.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ScheduledAccent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(R.string.scheduled_card_cancel),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }

    if (showingDetail) {
        ScheduledTaskDetailSheet(
            marker = marker,
            hostSessionId = hostSessionId,
            onDismiss = { showingDetail = false },
        )
    }

    if (confirming) {
        MinisAlertDialog(
            onDismissRequest = { confirming = false },
            title = stringResource(R.string.scheduled_card_cancel_confirm_title),
            text = stringResource(
                R.string.scheduled_card_cancel_confirm_body,
                marker.label.ifEmpty { marker.prompt.take(40) },
            ),
            confirmText = stringResource(R.string.scheduled_card_cancel_confirm_ok),
            dismissText = stringResource(R.string.scheduled_card_cancel_confirm_keep),
            isDestructive = true,
            onConfirm = {
                ScheduledTaskManager(context.applicationContext as Application)
                    .setEnabled(marker.taskId, false)
                cancelled = true
                confirming = false
            },
        )
    }
}
