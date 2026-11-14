package com.openminis.app.ui.chat

import com.openminis.app.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.ui.graphics.Color

internal val stepTimestampFormatter: java.text.SimpleDateFormat =
    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)

internal fun formatStepTimestamp(epochMs: Long): String =
    stepTimestampFormatter.format(java.util.Date(epochMs))

internal fun formatStepDuration(seconds: Long, stillRunning: Boolean): String {
    val safe = seconds.coerceAtLeast(0L)
    val base = when {
        safe < 60L -> "${safe}s"
        safe < 3600L -> {
            val m = safe / 60L
            val s = safe % 60L
            if (s == 0L) "${m}m" else "${m}m${s}s"
        }
        else -> {
            val h = safe / 3600L
            val m = (safe % 3600L) / 60L
            if (m == 0L) "${h}h" else "${h}h${m}m"
        }
    }
    return if (stillRunning) "$base…" else base
}

// Material 3 Expressive semantic tool accents
internal fun toolAccentColor(toolName: String): Color = when (toolName) {
    "shell_execute" -> Color(0xFF10B981)
    "file_read" -> Color(0xFF0EA5E9)
    "file_write" -> Color(0xFF4F46E5)
    "file_edit" -> Color(0xFFF59E0B)
    "browser_use" -> Color(0xFF3B82F6)
    "read_image" -> Color(0xFF9333EA)
    "memory_write", "memory_get" -> Color(0xFFEC4899)
    "web_search" -> Color(0xFF06B6D4)
    "ask_user" -> Color(0xFF6366F1)
    "subagent_task", "delegate_task", "agent_status" -> HelperAccentStatic
    else -> Color(0xFF64748B)
}

internal fun toolIconFor(toolName: String) = when (toolName) {
    "shell_execute" -> Icons.Default.Terminal
    "file_read" -> Icons.Default.Description
    "file_write" -> Icons.AutoMirrored.Filled.NoteAdd
    "file_edit" -> Icons.Default.EditNote
    "browser_use" -> Icons.Default.Language
    "read_image" -> Icons.Default.Image
    "memory_write", "memory_get" -> Icons.Default.Psychology
    "web_search" -> Icons.Default.Search
    "ask_user" -> Icons.Outlined.ChecklistRtl
    "subagent_task", "delegate_task", "agent_status" -> Icons.Default.Groups
    else -> Icons.Default.Build
}

@androidx.annotation.DrawableRes
internal fun toolIconResFor(toolName: String?): Int = when (toolName) {
    "shell_execute" -> R.drawable.ic_tool_terminal
    "file_read" -> R.drawable.ic_tool_description
    "file_write" -> R.drawable.ic_tool_note_add
    "file_edit" -> R.drawable.ic_tool_edit_note
    "browser_use" -> R.drawable.ic_tool_globe
    "read_image" -> R.drawable.ic_tool_image
    "memory_write", "memory_get" -> R.drawable.ic_tool_psychology
    "web_search" -> R.drawable.ic_tool_search
    "ask_user" -> R.drawable.ic_tool_questionnaire
    "subagent_task", "delegate_task", "agent_status" -> R.drawable.ic_tool_groups
    else -> R.drawable.ic_tool_build
}

@androidx.annotation.ColorInt
internal fun toolAccentColorInt(toolName: String?): Int = when (toolName) {
    "shell_execute" -> 0xFF10B981.toInt()
    "file_read" -> 0xFF0EA5E9.toInt()
    "file_write" -> 0xFF4F46E5.toInt()
    "file_edit" -> 0xFFF59E0B.toInt()
    "browser_use" -> 0xFF3B82F6.toInt()
    "read_image" -> 0xFF9333EA.toInt()
    "memory_write", "memory_get" -> 0xFFEC4899.toInt()
    "web_search" -> 0xFF06B6D4.toInt()
    "ask_user" -> 0xFF6366F1.toInt()
    "subagent_task", "delegate_task", "agent_status" -> 0xFF8B5CF6.toInt()
    else -> 0xFF64748B.toInt()
}

internal fun friendlyToolTitleFor(toolName: String?): String = when (toolName) {
    null, "" -> "Minis"
    "shell_execute" -> "Execute Shell"
    "file_read" -> "Read File"
    "file_write" -> "Write File"
    "file_edit" -> "Edit File"
    "browser_use" -> "Browse Web"
    "read_image" -> "Read Image"
    "memory_write" -> "Write Memory"
    "memory_get" -> "Read Memory"
    "web_search" -> "Search Web"
    "ask_user" -> "Ask User"
    else -> toolName
        .split('_')
        .filter { it.isNotEmpty() }
        .joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }
}

internal fun toolDisplayName(toolName: String): String = when (toolName) {
    "shell_execute" -> "terminal"
    "file_read" -> "file reader"
    "file_write" -> "file writer"
    "file_edit" -> "file editor"
    "browser_use" -> "browser"
    "read_image" -> "image viewer"
    "memory_write" -> "memory"
    "memory_get" -> "memory"
    "web_search" -> "search"
    "ask_user" -> "questionnaire"
    "subagent_task", "delegate_task", "agent_status" -> "agent"
    else -> toolName
}

internal fun toolTitleLabel(toolName: String): String = when (toolName) {
    "shell_execute" -> "Minis is using Shell"
    "file_read" -> "Minis is reading File"
    "file_write" -> "Minis is using Editor"
    "file_edit" -> "Minis is editing File"
    "browser_use" -> "Minis is using Browser"
    "read_image" -> "Minis is reading Image"
    "memory_write", "memory_get" -> "Minis is using Memory"
    "web_search" -> "Minis is using Search"
    "ask_user" -> "Minis is asking Questions"
    "subagent_task", "delegate_task", "agent_status" -> "Minis is using an Agent"
    else -> "Minis is using ${toolDisplayName(toolName)}"
}

internal fun formatToolDuration(ms: Long): String {
    val seconds = ms / 1000.0
    return when {
        seconds < 1 -> String.format("%.1fs", seconds)
        seconds < 60 -> String.format("%.0fs", seconds)
        else -> {
            val m = (seconds / 60).toInt()
            val s = (seconds % 60).toInt()
            "${m}m ${s}s"
        }
    }
}
