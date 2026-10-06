package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PersonaEntity
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
import java.util.UUID

@Composable
fun SettingsScreen(
    currentApiKey: String,
    onSaveApiKey: (String) -> Unit,
    onTestKey: suspend (String) -> Result<String>,
    temperature: Float,
    onSaveTemperature: (Float) -> Unit,
    topP: Float,
    onSaveTopP: (Float) -> Unit,
    topK: Int,
    onSaveTopK: (Int) -> Unit,
    maxTokens: Int,
    onSaveMaxTokens: (Int) -> Unit,
    isSafetyBlockNone: Boolean,
    onSaveSafetyBlockNone: (Boolean) -> Unit,
    personas: List<PersonaEntity>,
    activePersonaId: String,
    onSelectPersona: (String) -> Unit,
    onAddCustomPersona: (PersonaEntity) -> Unit,
    onExportJson: suspend () -> String,
    onExportMarkdown: suspend () -> String,
    onPurgeAllData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    BackHandler { onBack() }

    var apiKeyEdit by remember { mutableStateOf(currentApiKey) }
    var keyValidationStatus by remember { mutableStateOf<String?>(null) }
    var isTestingKey by remember { mutableStateOf(false) }

    var tempValue by remember { mutableFloatStateOf(temperature) }
    var topPValue by remember { mutableFloatStateOf(topP) }
    var topKValue by remember { mutableIntStateOf(topK) }
    var maxTokensValue by remember { mutableIntStateOf(maxTokens) }
    var safetyBlockNoneValue by remember { mutableStateOf(isSafetyBlockNone) }

    var showPurgeDialog by remember { mutableStateOf(false) }
    var showAddPersonaDialog by remember { mutableStateOf(false) }
    var newPersonaName by remember { mutableStateOf("") }
    var newPersonaTag by remember { mutableStateOf("") }
    var newPersonaPrompt by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Text(
                text = "ENGINE PARAMETERS",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AccentEmerald,
                letterSpacing = 1.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: LLM Engine Tuning
            SettingsCard(title = "1. NEURAL INFERENCE TUNING") {
                // Temperature Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Temperature", fontSize = 13.sp, color = TextPrimary)
                        Text(
                            text = String.format("%.2f", tempValue),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentEmerald
                        )
                    }
                    Slider(
                        value = tempValue,
                        onValueChange = {
                            tempValue = it
                            onSaveTemperature(it)
                        },
                        valueRange = 0.0f..2.0f,
                        steps = 20,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentEmerald,
                            activeTrackColor = AccentEmerald,
                            inactiveTrackColor = SurfaceHighlight
                        ),
                        modifier = Modifier.testTag("temperature_slider")
                    )
                }

                // Top-P Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Top-P (Nucleus Sampling)", fontSize = 13.sp, color = TextPrimary)
                        Text(
                            text = String.format("%.2f", topPValue),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentCyan
                        )
                    }
                    Slider(
                        value = topPValue,
                        onValueChange = {
                            topPValue = it
                            onSaveTopP(it)
                        },
                        valueRange = 0.1f..1.0f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentCyan,
                            activeTrackColor = AccentCyan,
                            inactiveTrackColor = SurfaceHighlight
                        ),
                        modifier = Modifier.testTag("top_p_slider")
                    )
                }

                // Top-K Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Top-K", fontSize = 13.sp, color = TextPrimary)
                        Text(
                            text = "$topKValue",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                    Slider(
                        value = topKValue.toFloat(),
                        onValueChange = {
                            topKValue = it.toInt()
                            onSaveTopK(it.toInt())
                        },
                        valueRange = 1f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = TextPrimary,
                            activeTrackColor = TextPrimary,
                            inactiveTrackColor = SurfaceHighlight
                        ),
                        modifier = Modifier.testTag("top_k_slider")
                    )
                }

                // Max Output Tokens Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Max Output Tokens", fontSize = 13.sp, color = TextPrimary)
                        Text(
                            text = "$maxTokensValue tokens",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentEmerald
                        )
                    }
                    Slider(
                        value = maxTokensValue.toFloat(),
                        onValueChange = {
                            maxTokensValue = it.toInt()
                            onSaveMaxTokens(it.toInt())
                        },
                        valueRange = 512f..8192f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentEmerald,
                            activeTrackColor = AccentEmerald,
                            inactiveTrackColor = SurfaceHighlight
                        ),
                        modifier = Modifier.testTag("max_tokens_slider")
                    )
                }

                // Safety Settings Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Safety Threshold: Block None", fontSize = 13.sp, color = TextPrimary)
                        Text(
                            text = "Unrestrained system queries for developers & security research.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = safetyBlockNoneValue,
                        onCheckedChange = {
                            safetyBlockNoneValue = it
                            onSaveSafetyBlockNone(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentEmerald,
                            checkedTrackColor = SurfaceHighlight,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceGraphite
                        ),
                        modifier = Modifier.testTag("safety_switch")
                    )
                }
            }

            // Section 2: System Persona Hub
            SettingsCard(title = "2. SYSTEM PERSONA HUB") {
                for (p in personas) {
                    val isSelected = p.id == activePersonaId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SurfaceHighlight else SurfaceGraphite)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) AccentEmerald else SurfaceBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectPersona(p.id)
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AccentEmerald else SurfaceBorder)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = p.name,
                                    fontSize = 13.sp,
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
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Button(
                    onClick = { showAddPersonaDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceHighlight,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_persona_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("ADD CUSTOM PERSONA", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Section 3: API Key Management
            SettingsCard(title = "3. CRYPTOGRAPHIC CORE ACCESS") {
                OutlinedTextField(
                    value = apiKeyEdit,
                    onValueChange = {
                        apiKeyEdit = it
                        keyValidationStatus = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_api_key_field"),
                    placeholder = { Text("Enter Gemini API Key...", color = TextSubtle, fontSize = 12.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        fontSize = 12.sp
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedContainerColor = SurfaceGraphite,
                        unfocusedContainerColor = SurfaceGraphite
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (keyValidationStatus != null) {
                        Text(
                            text = keyValidationStatus.orEmpty(),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AccentEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (apiKeyEdit.isNotBlank()) {
                                    isTestingKey = true
                                    scope.launch {
                                        val res = onTestKey(apiKeyEdit.trim())
                                        isTestingKey = false
                                        res.onSuccess {
                                            keyValidationStatus = "200 OK • Core Ready"
                                            onSaveApiKey(apiKeyEdit.trim())
                                        }.onFailure {
                                            keyValidationStatus = "Error: ${it.message?.take(25)}"
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceHighlight,
                                contentColor = AccentEmerald
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isTestingKey && apiKeyEdit.isNotBlank(),
                            modifier = Modifier.testTag("test_key_settings_button")
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = AccentEmerald,
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Text("PING", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Button(
                            onClick = {
                                onSaveApiKey(apiKeyEdit.trim())
                                Toast.makeText(context, "API Key Saved", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentEmerald,
                                contentColor = OledBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("save_key_settings_button")
                        ) {
                            Text("SAVE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section 4: Local Database & Export
            SettingsCard(title = "4. LOCAL-FIRST DATABASE") {
                Text(
                    text = "Encrypted local SQLite. No cloud backups, no telemetry.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val json = onExportJson()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("kernel_export.json", json))
                                Toast.makeText(context, "Exported JSON copied to clipboard", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceHighlight,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_json_button")
                    ) {
                        Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("EXPORT JSON", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val md = onExportMarkdown()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("kernel_export.md", md))
                                Toast.makeText(context, "Exported Markdown copied to clipboard", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceHighlight,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_md_button")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("EXPORT MD", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Button(
                    onClick = { showPurgeDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceHighlight,
                        contentColor = AccentWarning
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("purge_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("PURGE ALL LOCAL DATA", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Purge Confirmation Dialog
    if (showPurgeDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            containerColor = SurfaceGraphite,
            title = {
                Text(
                    text = "Purge All Data?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentWarning
                )
            },
            text = {
                Text(
                    text = "This will erase all local conversations, messages, attachments and reset settings. This action is irreversible.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPurgeAllData()
                        showPurgeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentWarning, contentColor = OledBackground)
                ) {
                    Text("PURGE EVERYTHING")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Add Persona Dialog
    if (showAddPersonaDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonaDialog = false },
            containerColor = SurfaceGraphite,
            title = {
                Text(
                    text = "Create Custom Intellect Persona",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPersonaName,
                        onValueChange = { newPersonaName = it },
                        label = { Text("Persona Name (e.g. Rust Auditor)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = SurfaceBorder
                        )
                    )
                    OutlinedTextField(
                        value = newPersonaTag,
                        onValueChange = { newPersonaTag = it },
                        label = { Text("Tag (e.g. RUST)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = SurfaceBorder
                        )
                    )
                    OutlinedTextField(
                        value = newPersonaPrompt,
                        onValueChange = { newPersonaPrompt = it },
                        label = { Text("System Directive / Instructions") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = SurfaceBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPersonaName.isNotBlank() && newPersonaPrompt.isNotBlank()) {
                            onAddCustomPersona(
                                PersonaEntity(
                                    id = "custom_" + UUID.randomUUID().toString().take(8),
                                    name = newPersonaName.trim(),
                                    tag = if (newPersonaTag.isNotBlank()) newPersonaTag.uppercase() else "CUSTOM",
                                    description = "Custom user persona",
                                    systemPrompt = newPersonaPrompt.trim(),
                                    isCustom = true
                                )
                            )
                            showAddPersonaDialog = false
                            newPersonaName = ""
                            newPersonaTag = ""
                            newPersonaPrompt = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = OledBackground)
                ) {
                    Text("CREATE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonaDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = AccentEmerald,
            letterSpacing = 1.sp
        )
        content()
    }
}
