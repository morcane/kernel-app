package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Attachment
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentEmeraldDark
import com.example.ui.theme.AccentWarning
import com.example.ui.theme.OledBackground
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGraphite
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import kotlin.math.sin

@Composable
fun FloatingInputDock(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isStreaming: Boolean,
    attachments: List<Attachment>,
    onAddAttachment: (Attachment) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onClipboardPaste: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }

    // Visual media picker (Images)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            onAddAttachment(
                Attachment(
                    fileName = "image_${System.currentTimeMillis() % 10000}.jpg",
                    mimeType = "image/jpeg",
                    fileSizeFormatted = "1.2 MB",
                    localUri = it.toString(),
                    isImage = true
                )
            )
        }
    }

    // Document/Code file picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment ?: "document.txt"
            onAddAttachment(
                Attachment(
                    fileName = name,
                    mimeType = "text/plain",
                    fileSizeFormatted = "14 KB",
                    localUri = it.toString(),
                    lineCount = 120,
                    isImage = false
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Attachment Tray
        if (attachments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (att in attachments) {
                    AttachmentChip(
                        attachment = att,
                        onRemove = { onRemoveAttachment(att.id) }
                    )
                }
            }
        }

        // Floating Dock Container with frosted border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceElevated.copy(alpha = 0.95f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(SurfaceHighlight, SurfaceBorder)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column {
                // Audio Waveform overlay when recording
                AnimatedVisibility(
                    visible = isRecordingAudio,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    AudioWaveformBar(
                        onCancel = {
                            isRecordingAudio = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Attachment (+) Button
                    Box {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showAttachmentMenu = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("attach_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add attachments",
                                tint = AccentEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showAttachmentMenu,
                            onDismissRequest = { showAttachmentMenu = false },
                            modifier = Modifier
                                .background(SurfaceGraphite)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Gallery / Images", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = AccentEmerald)
                                },
                                onClick = {
                                    showAttachmentMenu = false
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Document / Code File", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = AccentCyan)
                                },
                                onClick = {
                                    showAttachmentMenu = false
                                    documentPickerLauncher.launch("*/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Paste Clipboard", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = TextSecondary)
                                },
                                onClick = {
                                    showAttachmentMenu = false
                                    onClipboardPaste()
                                }
                            )
                        }
                    }

                    // Auto-expanding Input Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 130.dp)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = "Enter neural directive or code...",
                                color = TextSubtle,
                                fontSize = 14.5.sp
                            )
                        }
                        BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 14.5.sp,
                                lineHeight = 21.sp
                            ),
                            cursorBrush = SolidColor(AccentEmerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_input_field")
                        )
                    }

                    // Voice Waveform Trigger Button
                    IconButton(
                        onClick = {
                            isRecordingAudio = !isRecordingAudio
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isRecordingAudio) {
                                onTextChange(if (text.isEmpty()) "Analyze system performance and explain architecture." else text)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Directive",
                            tint = if (isRecordingAudio) AccentEmerald else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Morphing Send / Stop Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isStreaming) SurfaceHighlight else AccentEmerald)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isStreaming) {
                                    onStop()
                                } else {
                                    if (text.isNotBlank() || attachments.isNotEmpty()) {
                                        onSend()
                                    }
                                }
                            }
                            .testTag("send_morph_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isStreaming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = AccentEmerald,
                                strokeWidth = 2.dp
                            )
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Interrupt Generation",
                                tint = AccentEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Transmit",
                                tint = OledBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentChip(
    attachment: Attachment,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (attachment.isImage && attachment.localUri != null) {
            AsyncImage(
                model = attachment.localUri,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        Column {
            Text(
                text = attachment.fileName,
                fontSize = 11.sp,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                text = if (attachment.lineCount != null) "${attachment.lineCount} lines • ${attachment.fileSizeFormatted}" else attachment.fileSizeFormatted,
                fontSize = 9.5.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }

        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove",
            tint = TextSubtle,
            modifier = Modifier
                .size(14.dp)
                .clickable { onRemove() }
        )
    }
}

@Composable
fun AudioWaveformBar(onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AccentEmerald)
            )
            Text(
                text = "REC AUDIO // WAVEFORM ACTIVE",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = AccentEmerald
            )
        }

        Canvas(
            modifier = Modifier
                .width(140.dp)
                .height(20.dp)
        ) {
            val width = size.width
            val height = size.height
            val midY = height / 2f
            val bars = 24
            val barWidth = width / bars

            for (i in 0 until bars) {
                val amp = (sin(phase + i * 0.4f) + 1f) / 2f * (height * 0.7f)
                val x = i * barWidth
                drawLine(
                    color = AccentEmerald,
                    start = Offset(x, midY - amp / 2),
                    end = Offset(x, midY + amp / 2),
                    strokeWidth = 2.5f
                )
            }
        }

        Text(
            text = "CANCEL",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            modifier = Modifier.clickable { onCancel() }
        )
    }
}
