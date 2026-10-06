package com.example.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AVAILABLE_MODELS
import com.example.data.model.Attachment
import com.example.data.model.MessageEntity
import com.example.data.model.SessionEntity
import com.example.ui.components.FloatingInputDock
import com.example.ui.components.MarkdownRenderer
import com.example.ui.components.ModelSelectorSheet
import com.example.ui.components.ThinkingAccordion
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentSession: SessionEntity?,
    messages: List<MessageEntity>,
    isStreaming: Boolean,
    streamingContent: String,
    streamingThinking: String,
    onSendMessage: (String, List<Attachment>) -> Unit,
    onStopStreaming: () -> Unit,
    onOpenDrawer: () -> Unit,
    onClearContext: () -> Unit,
    onSelectModel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showModelSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var inputText by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }

    // Scroll to bottom when new messages arrive or while streaming
    LaunchedEffect(messages.size, streamingContent.length) {
        if (messages.isNotEmpty() || streamingContent.isNotEmpty()) {
            val total = messages.size + if (isStreaming) 1 else 0
            if (total > 0) {
                listState.animateScrollToItem(total - 1)
            }
        }
    }

    val currentModel = AVAILABLE_MODELS.find { it.id == currentSession?.modelId }
        ?: AVAILABLE_MODELS.first()

    // Token estimation calculation
    val sessionTokens = currentSession?.totalTokens ?: 0L
    val tokenText = if (sessionTokens > 1000) String.format("%.1fk", sessionTokens / 1000.0) else "$sessionTokens"
    val costEstimate = String.format("$%.3f", (sessionTokens / 1_000_000.0) * 0.15)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Two-line parallel drawer toggle icon (― ⎼)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenDrawer()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("drawer_toggle_button")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.width(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(TextPrimary)
                        )
                        Box(
                            modifier = Modifier
                                .width(11.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(AccentEmerald)
                        )
                    }
                }

                // Clickable Model Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showModelSheet = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("model_selector_chip"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald)
                    )
                    Text(
                        text = currentModel.displayName,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceHighlight)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = currentModel.contextWindow,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentCyan
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Token counter & Clear button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Tokens: $tokenText",
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                        Text(
                            text = costEstimate,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSubtle
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClearContext()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("clear_context_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Context",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Message List Surface
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty() && !isStreaming) {
                    // Empty State: Minimalist Terminal
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevated)
                                .border(1.dp, SurfaceBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "KERNEL COMPUTING READY",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Direct connection to ${currentModel.displayName}. Enter an architectural query, paste code, or attach multimodal data.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageItem(message = msg)
                        }

                        if (isStreaming) {
                            item(key = "streaming_node") {
                                StreamingModelMessageItem(
                                    content = streamingContent,
                                    thinking = streamingThinking
                                )
                            }
                        }

                        // Bottom padding for floating dock
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }

        // Floating Dynamic Input Dock at bottom
        FloatingInputDock(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                val currentText = inputText
                val currentAtts = attachments
                inputText = ""
                attachments = emptyList()
                onSendMessage(currentText, currentAtts)
            },
            onStop = onStopStreaming,
            isStreaming = isStreaming,
            attachments = attachments,
            onAddAttachment = { attachments = attachments + it },
            onRemoveAttachment = { id -> attachments = attachments.filter { it.id != id } },
            onClipboardPaste = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                if (!clip.isNullOrBlank()) {
                    inputText = if (inputText.isEmpty()) clip else "$inputText\n$clip"
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Model Selector Sheet
        if (showModelSheet) {
            ModelSelectorSheet(
                sheetState = sheetState,
                currentModelId = currentSession?.modelId ?: "gemini-2.5-flash",
                onSelectModel = {
                    onSelectModel(it)
                    showModelSheet = false
                },
                onDismiss = { showModelSheet = false }
            )
        }
    }
}

@Composable
fun MessageItem(message: MessageEntity) {
    if (message.role == "user") {
        // User Message: Right-aligned, #16181D surface, rounded 18dp (bottom-right 4dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // Attached files/images inside message
            val atts = parseAttachments(message.attachmentsJson)
            if (atts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (att in atts) {
                        if (att.isImage && att.localUri != null) {
                            AsyncImage(
                                model = att.localUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceElevated)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${att.fileName} (${att.fileSizeFormatted})",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentCyan
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(SurfaceElevated)
                    .border(
                        1.dp,
                        SurfaceBorder,
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                    color = TextPrimary
                )
            }
        }
    } else {
        // Model (Kernel) Message: Borderless on pure black surface #000000, editorial magazine layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Subtle left accent status line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(SurfaceHighlight)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Thinking process accordion if present
                if (!message.thinkingProcess.isNullOrBlank()) {
                    ThinkingAccordion(thinkingProcess = message.thinkingProcess)
                }

                // High fidelity Markdown content
                MarkdownRenderer(content = message.content)
            }
        }
    }
}

@Composable
fun StreamingModelMessageItem(
    content: String,
    thinking: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Animated Emerald Pulse Status Line
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.dp))
                .background(AccentEmerald.copy(alpha = pulseAlpha))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (thinking.isNotBlank()) {
                ThinkingAccordion(thinkingProcess = thinking, durationText = "Active...")
            }

            if (content.isNotBlank()) {
                MarkdownRenderer(content = content)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = AccentEmerald,
                        strokeWidth = 1.5.dp
                    )
                    Text(
                        text = "NEURAL CORE STREAMING...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        color = AccentEmerald
                    )
                }
            }
        }
    }
}

fun parseAttachments(json: String): List<Attachment> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<Attachment>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Attachment(
                    id = obj.optString("id"),
                    fileName = obj.optString("fileName"),
                    mimeType = obj.optString("mimeType"),
                    fileSizeFormatted = obj.optString("fileSizeFormatted"),
                    localUri = obj.optString("localUri").takeIf { it.isNotEmpty() },
                    isImage = obj.optBoolean("isImage", false)
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}
