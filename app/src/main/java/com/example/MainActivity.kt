package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AVAILABLE_MODELS
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.drawer.SessionDrawerContent
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.viewmodel.KernelScreen
import com.example.ui.screens.viewmodel.KernelViewModel
import com.example.ui.theme.KernelTheme
import com.example.ui.theme.OledBackground
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: KernelViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val sessions by viewModel.allSessions.collectAsState()
            val messages by viewModel.currentMessages.collectAsState()
            val personas by viewModel.allPersonas.collectAsState()

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            KernelTheme(useMonetDynamic = uiState.isMonetEnabled) {
                Crossfade(
                    targetState = uiState.currentScreen,
                    animationSpec = tween(350),
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        KernelScreen.ONBOARDING -> {
                            OnboardingScreen(
                                onInitializeCore = { apiKey, personaId ->
                                    viewModel.completeOnboarding(apiKey, personaId)
                                },
                                onTestKey = { key ->
                                    viewModel.testApiKey(key)
                                }
                            )
                        }

                        KernelScreen.CHAT -> {
                            BackHandler(enabled = drawerState.isOpen) {
                                scope.launch { drawerState.close() }
                            }

                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                gesturesEnabled = true,
                                scrimColor = OledBackground.copy(alpha = 0.75f),
                                drawerContent = {
                                    ModalDrawerSheet(
                                        drawerContainerColor = OledBackground
                                    ) {
                                        SessionDrawerContent(
                                            sessions = sessions,
                                            currentSessionId = uiState.currentSessionId,
                                            onSelectSession = { id ->
                                                viewModel.selectSession(id)
                                                scope.launch { drawerState.close() }
                                            },
                                            onNewSession = {
                                                viewModel.createNewSession()
                                                scope.launch { drawerState.close() }
                                            },
                                            onDeleteSession = { id ->
                                                viewModel.deleteSession(id)
                                            },
                                            onTogglePinSession = { id ->
                                                viewModel.togglePinSession(id)
                                            },
                                            onRenameSession = { id, title ->
                                                viewModel.renameSession(id, title)
                                            },
                                            onOpenSettings = {
                                                scope.launch { drawerState.close() }
                                                viewModel.navigateTo(KernelScreen.SETTINGS)
                                            },
                                            isMonetEnabled = uiState.isMonetEnabled,
                                            onToggleMonet = { viewModel.toggleMonetTheme(it) }
                                        )
                                    }
                                }
                            ) {
                                val currentSession = sessions.find { it.id == uiState.currentSessionId }
                                    ?: sessions.firstOrNull()

                                ChatScreen(
                                    currentSession = currentSession,
                                    messages = messages,
                                    isStreaming = uiState.isStreaming,
                                    streamingContent = uiState.streamingContent,
                                    streamingThinking = uiState.streamingThinking,
                                    onSendMessage = { prompt, atts ->
                                        viewModel.sendMessage(prompt, atts)
                                    },
                                    onStopStreaming = { viewModel.stopStreaming() },
                                    onOpenDrawer = {
                                        scope.launch { drawerState.open() }
                                    },
                                    onClearContext = { viewModel.clearContext() },
                                    onSelectModel = { modelId -> viewModel.selectModel(modelId) }
                                )
                            }
                        }

                        KernelScreen.SETTINGS -> {
                            SettingsScreen(
                                currentApiKey = uiState.apiKey,
                                onSaveApiKey = { viewModel.updateApiKey(it) },
                                onTestKey = { key -> viewModel.testApiKey(key) },
                                temperature = uiState.temperature,
                                onSaveTemperature = { viewModel.updateTemperature(it) },
                                topP = uiState.topP,
                                onSaveTopP = { viewModel.updateTopP(it) },
                                topK = uiState.topK,
                                onSaveTopK = { viewModel.updateTopK(it) },
                                maxTokens = uiState.maxTokens,
                                onSaveMaxTokens = { viewModel.updateMaxTokens(it) },
                                isSafetyBlockNone = uiState.isSafetyBlockNone,
                                onSaveSafetyBlockNone = { viewModel.updateSafetyBlockNone(it) },
                                personas = personas,
                                activePersonaId = uiState.activePersonaId,
                                onSelectPersona = { viewModel.selectPersona(it) },
                                onAddCustomPersona = { viewModel.addCustomPersona(it) },
                                onExportJson = { viewModel.exportJson() },
                                onExportMarkdown = { viewModel.exportMarkdown() },
                                onPurgeAllData = { viewModel.purgeAllData() },
                                onBack = { viewModel.navigateTo(KernelScreen.CHAT) }
                            )
                        }
                    }
                }
            }
        }
    }
}
