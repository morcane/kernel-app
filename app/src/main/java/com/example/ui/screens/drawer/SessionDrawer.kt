package com.example.ui.screens.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SessionEntity
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.OledBackground
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGraphite
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import java.util.concurrent.TimeUnit

@Composable
fun SessionDrawerContent(
    sessions: List<SessionEntity>,
    currentSessionId: String?,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onTogglePinSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    isMonetEnabled: Boolean,
    onToggleMonet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var sessionToRename by remember { mutableStateOf<SessionEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    val filteredSessions = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) sessions
        else sessions.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    // Grouping
    val pinned = filteredSessions.filter { it.isPinned }
    val now = System.currentTimeMillis()
    val unpinned = filteredSessions.filter { !it.isPinned }

    val today = unpinned.filter { now - it.updatedAt < TimeUnit.DAYS.toMillis(1) }
    val yesterday = unpinned.filter {
        val diff = now - it.updatedAt
        diff >= TimeUnit.DAYS.toMillis(1) && diff < TimeUnit.DAYS.toMillis(2)
    }
    val previous7Days = unpinned.filter {
        val diff = now - it.updatedAt
        diff >= TimeUnit.DAYS.toMillis(2) && diff < TimeUnit.DAYS.toMillis(7)
    }
    val older = unpinned.filter { now - it.updatedAt >= TimeUnit.DAYS.toMillis(7) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(SurfaceGraphite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Drawer Header & New Chat Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AccentEmerald)
                )
                Text(
                    text = "KERNEL SESSIONS",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AccentEmerald,
                    letterSpacing = 1.sp
                )
            }

            // New Chat Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNewSession()
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .testTag("new_session_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Session",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar (Instant Local Search)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSubtle,
                    modifier = Modifier.size(16.dp)
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    cursorBrush = SolidColor(AccentEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_search_field"),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search local threads...",
                                color = TextSubtle,
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sessions List with Sections
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (pinned.isNotEmpty()) {
                item { SectionHeader("PINNED CORES") }
                items(pinned, key = { it.id }) { s ->
                    DrawerSessionItem(
                        session = s,
                        isSelected = s.id == currentSessionId,
                        onSelect = { onSelectSession(s.id) },
                        onTogglePin = { onTogglePinSession(s.id) },
                        onRename = {
                            sessionToRename = s
                            renameText = s.title
                        },
                        onDelete = { onDeleteSession(s.id) }
                    )
                }
            }

            if (today.isNotEmpty()) {
                item { SectionHeader("TODAY") }
                items(today, key = { it.id }) { s ->
                    DrawerSessionItem(
                        session = s,
                        isSelected = s.id == currentSessionId,
                        onSelect = { onSelectSession(s.id) },
                        onTogglePin = { onTogglePinSession(s.id) },
                        onRename = {
                            sessionToRename = s
                            renameText = s.title
                        },
                        onDelete = { onDeleteSession(s.id) }
                    )
                }
            }

            if (yesterday.isNotEmpty()) {
                item { SectionHeader("YESTERDAY") }
                items(yesterday, key = { it.id }) { s ->
                    DrawerSessionItem(
                        session = s,
                        isSelected = s.id == currentSessionId,
                        onSelect = { onSelectSession(s.id) },
                        onTogglePin = { onTogglePinSession(s.id) },
                        onRename = {
                            sessionToRename = s
                            renameText = s.title
                        },
                        onDelete = { onDeleteSession(s.id) }
                    )
                }
            }

            if (previous7Days.isNotEmpty()) {
                item { SectionHeader("PREVIOUS 7 DAYS") }
                items(previous7Days, key = { it.id }) { s ->
                    DrawerSessionItem(
                        session = s,
                        isSelected = s.id == currentSessionId,
                        onSelect = { onSelectSession(s.id) },
                        onTogglePin = { onTogglePinSession(s.id) },
                        onRename = {
                            sessionToRename = s
                            renameText = s.title
                        },
                        onDelete = { onDeleteSession(s.id) }
                    )
                }
            }

            if (older.isNotEmpty()) {
                item { SectionHeader("OLDER") }
                items(older, key = { it.id }) { s ->
                    DrawerSessionItem(
                        session = s,
                        isSelected = s.id == currentSessionId,
                        onSelect = { onSelectSession(s.id) },
                        onTogglePin = { onTogglePinSession(s.id) },
                        onRename = {
                            sessionToRename = s
                            renameText = s.title
                        },
                        onDelete = { onDeleteSession(s.id) }
                    )
                }
            }
        }

        HorizontalDivider(
            color = SurfaceBorder,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Bottom Controls: Theme switch and Settings
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Theme Monet / OLED toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isMonetEnabled) "Monet Tint (On)" else "True OLED Black",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
                Switch(
                    checked = isMonetEnabled,
                    onCheckedChange = { onToggleMonet(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentEmerald,
                        checkedTrackColor = SurfaceHighlight,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceGraphite
                    ),
                    modifier = Modifier.testTag("theme_monet_switch")
                )
            }

            // Settings Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSettings()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("open_settings_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Engine Parameters",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "CONFIG",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AccentEmerald
                )
            }
        }
    }

    // Rename Session Dialog
    if (sessionToRename != null) {
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            containerColor = SurfaceGraphite,
            title = {
                Text(
                    text = "Rename Thread",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = sessionToRename
                        if (target != null && renameText.isNotBlank()) {
                            onRenameSession(target.id, renameText.trim())
                        }
                        sessionToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = OledBackground)
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = TextSubtle,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
fun DrawerSessionItem(
    session: SessionEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SurfaceElevated else OledBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentEmerald.copy(alpha = 0.6f) else SurfaceBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .testTag("session_item_${session.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (session.isPinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = AccentEmerald,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = session.title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) AccentEmerald else TextPrimary,
                    maxLines = 1
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (session.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin/Unpin",
                    tint = if (session.isPinned) AccentEmerald else TextSubtle,
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(
                onClick = onRename,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint = TextSubtle,
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextSubtle,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
