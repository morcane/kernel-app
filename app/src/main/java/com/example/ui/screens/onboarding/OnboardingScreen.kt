package com.example.ui.screens.onboarding

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.DEFAULT_PERSONAS
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentWarning
import com.example.ui.theme.OledBackground
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceGraphite
import com.example.ui.theme.SurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onInitializeCore: (apiKey: String, selectedPersonaId: String) -> Unit,
    onTestKey: suspend (String) -> Result<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var apiKeyInput by remember { mutableStateOf("") }
    var selectedPersonaId by remember { mutableStateOf("default_core") }
    var isValidating by remember { mutableStateOf(false) }
    var validationStatus by remember { mutableStateOf<String?>(null) }
    var isKeyValid by remember { mutableStateOf(false) }

    // Check if injected key exists
    LaunchedEffect(Unit) {
        val injected = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        if (injected.isNotBlank() && injected != "MY_GEMINI_API_KEY") {
            apiKeyInput = injected
            isValidating = true
            val res = onTestKey(injected)
            isValidating = false
            res.onSuccess {
                validationStatus = "200 OK • Core Online (Injected Secret)"
                isKeyValid = true
            }.onFailure {
                validationStatus = "Pre-check: ${it.message ?: "Key ready"}"
                isKeyValid = true
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "core_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Cinematic Glowing Computing Core Animation
            Box(
                modifier = Modifier
                    .size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val baseRadius = size.width / 2.8f * pulseScale

                    // Ambient Outer Radial Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentEmerald.copy(alpha = 0.25f),
                                AccentCyan.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseRadius * 1.5f
                        ),
                        radius = baseRadius * 1.5f,
                        center = center
                    )

                    // Core Outer Ring
                    drawCircle(
                        color = AccentEmerald.copy(alpha = 0.6f),
                        radius = baseRadius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )

                    // Inner Core Node
                    drawCircle(
                        color = AccentEmerald,
                        radius = 16.dp.toPx(),
                        center = center
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = center
                    )
                }
            }

            // Title & Philosophy
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "KERNEL",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 6.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Local-First Intelligence Engine",
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AccentEmerald,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Decentralized neural computing client. Zero telemetry. All sessions, system keys, and embeddings stored strictly on-device in encrypted SQLite.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 10.dp, start = 12.dp, end = 12.dp)
                )
            }

            // Key Input Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "GEMINI API KEY",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }

                    // Paste Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceHighlight)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrBlank()) {
                                    apiKeyInput = clip.trim()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("paste_key_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "PASTE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentCyan
                        )
                    }
                }

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        validationStatus = null
                        isKeyValid = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    placeholder = {
                        Text(
                            text = "AIzaSy...",
                            color = TextSubtle,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedContainerColor = SurfaceGraphite,
                        unfocusedContainerColor = SurfaceGraphite
                    )
                )

                // Live Validation Ping Button & Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (validationStatus != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isKeyValid) AccentEmerald else AccentWarning)
                            )
                            Text(
                                text = validationStatus.orEmpty(),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isKeyValid) AccentEmerald else AccentWarning,
                                maxLines = 1
                            )
                        }
                    } else {
                        Text(
                            text = "Requires active Gemini 2.5 / 2.0 access",
                            fontSize = 11.sp,
                            color = TextSubtle,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Test Key Ping Button
                    Button(
                        onClick = {
                            if (apiKeyInput.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isValidating = true
                                scope.launch {
                                    val res = onTestKey(apiKeyInput.trim())
                                    isValidating = false
                                    res.onSuccess {
                                        validationStatus = "200 OK • CORE READY"
                                        isKeyValid = true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }.onFailure {
                                        validationStatus = "Error: ${it.message?.take(28) ?: "Failed"}"
                                        isKeyValid = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceHighlight,
                            contentColor = AccentEmerald
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isValidating && apiKeyInput.isNotBlank(),
                        modifier = Modifier.testTag("verify_key_button")
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = AccentEmerald,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "PING CORE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Default Persona Selection
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DEFAULT INTELLECT PERSONA",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                for (p in DEFAULT_PERSONAS) {
                    val isSelected = p.id == selectedPersonaId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SurfaceElevated else SurfaceGraphite)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentEmerald else SurfaceBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedPersonaId = p.id
                            }
                            .padding(12.dp)
                            .testTag("persona_choice_${p.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AccentEmerald else SurfaceHighlight)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = p.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "[${p.tag}]",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentCyan
                                )
                            }
                            Text(
                                text = p.description,
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Initialize Core Primary Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onInitializeCore(apiKeyInput.trim(), selectedPersonaId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("initialize_core_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentEmerald,
                    contentColor = OledBackground
                )
            ) {
                Text(
                    text = "INITIALIZE CORE // LAUNCH",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
