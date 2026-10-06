package com.example.ui.screens.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Attachment
import com.example.data.model.MessageEntity
import com.example.data.model.PersonaEntity
import com.example.data.model.SessionEntity
import com.example.data.network.StreamChunk
import com.example.data.repository.KernelRepository
import com.example.data.security.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class KernelScreen {
    ONBOARDING,
    CHAT,
    SETTINGS
}

data class KernelUiState(
    val currentScreen: KernelScreen = KernelScreen.ONBOARDING,
    val currentSessionId: String? = null,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val streamingThinking: String = "",
    val errorMessage: String? = null,
    val isMonetEnabled: Boolean = false,
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxTokens: Int = 4096,
    val isSafetyBlockNone: Boolean = true,
    val activePersonaId: String = "default_core",
    val apiKey: String = ""
)

class KernelViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val prefs = PreferencesManager(application)
    private val repository = KernelRepository(db, prefs)

    val allSessions: StateFlow<List<SessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPersonas: StateFlow<List<PersonaEntity>> = repository.allPersonas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        KernelUiState(
            currentScreen = if (prefs.isOnboardingCompleted) KernelScreen.CHAT else KernelScreen.ONBOARDING,
            isMonetEnabled = prefs.isMonetThemeEnabled,
            temperature = prefs.temperature,
            topP = prefs.topP,
            topK = prefs.topK,
            maxTokens = prefs.maxOutputTokens,
            isSafetyBlockNone = prefs.isSafetyBlockNone,
            activePersonaId = prefs.activePersonaId,
            apiKey = prefs.customApiKey
        )
    )
    val uiState: StateFlow<KernelUiState> = _uiState.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<MessageEntity>> = _currentMessages.asStateFlow()

    private var streamingJob: Job? = null
    private var observeMessagesJob: Job? = null

    init {
        viewModelScope.launch {
            allSessions.collect { sessions ->
                if (_uiState.value.currentSessionId == null && sessions.isNotEmpty()) {
                    selectSession(sessions.first().id)
                } else if (sessions.isEmpty() && prefs.isOnboardingCompleted) {
                    createNewSession()
                }
            }
        }
    }

    fun completeOnboarding(apiKey: String, selectedPersonaId: String) {
        if (apiKey.isNotBlank()) {
            prefs.customApiKey = apiKey
        }
        prefs.activePersonaId = selectedPersonaId
        prefs.isOnboardingCompleted = true

        _uiState.value = _uiState.value.copy(
            currentScreen = KernelScreen.CHAT,
            activePersonaId = selectedPersonaId,
            apiKey = apiKey
        )

        viewModelScope.launch {
            if (allSessions.value.isEmpty()) {
                createNewSession()
            }
        }
    }

    fun navigateTo(screen: KernelScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun createNewSession(title: String = "Neural Query") {
        viewModelScope.launch {
            val newSession = repository.createNewSession(title = title)
            selectSession(newSession.id)
        }
    }

    fun selectSession(sessionId: String) {
        _uiState.value = _uiState.value.copy(currentSessionId = sessionId)
        observeMessagesJob?.cancel()
        observeMessagesJob = viewModelScope.launch {
            repository.getMessagesForSession(sessionId).collect { msgs ->
                _currentMessages.value = msgs
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                val remaining = allSessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first().id)
                } else {
                    createNewSession()
                }
            }
        }
    }

    fun togglePinSession(sessionId: String) {
        viewModelScope.launch {
            repository.togglePinSession(sessionId)
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, newTitle)
        }
    }

    fun clearContext() {
        val sessId = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            repository.clearCurrentSessionMessages(sessId)
        }
    }

    fun selectModel(modelId: String) {
        prefs.currentModelId = modelId
        val sessId = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            val sess = repository.getSession(sessId) ?: return@launch
            db.sessionDao().updateSession(sess.copy(modelId = modelId))
        }
    }

    fun sendMessage(userPrompt: String, attachments: List<Attachment>) {
        val sessId = _uiState.value.currentSessionId ?: return
        if (userPrompt.isBlank() && attachments.isEmpty()) return

        // 1. Persist User Message
        val attsJson = serializeAttachments(attachments)
        val userMsg = MessageEntity(
            sessionId = sessId,
            role = "user",
            content = userPrompt,
            attachmentsJson = attsJson
        )

        viewModelScope.launch {
            repository.insertMessage(userMsg)

            // Update session title if first message
            val currentList = _currentMessages.value
            if (currentList.size <= 1) {
                val autoTitle = if (userPrompt.isNotBlank()) {
                    userPrompt.take(28).trim()
                } else {
                    attachments.firstOrNull()?.fileName ?: "Multimodal Input"
                }
                repository.updateSessionTitle(sessId, autoTitle)
            }

            // 2. Prepare History & Trigger Stream
            val history = currentList.map { it.role to it.content }
            val systemPrompt = repository.getActivePersonaPrompt()

            _uiState.value = _uiState.value.copy(
                isStreaming = true,
                streamingContent = "",
                streamingThinking = "",
                errorMessage = null
            )

            val accumulatedContent = StringBuilder()
            val accumulatedThinking = StringBuilder()
            var tokensUsed = 0

            streamingJob?.cancel()
            streamingJob = viewModelScope.launch {
                try {
                    repository.streamGeminiResponse(
                        sessionId = sessId,
                        prompt = userPrompt,
                        attachments = attachments,
                        history = history,
                        systemPrompt = systemPrompt
                    ).collect { chunk ->
                        when (chunk) {
                            is StreamChunk.Text -> {
                                accumulatedContent.append(chunk.content)
                                _uiState.value = _uiState.value.copy(
                                    streamingContent = accumulatedContent.toString()
                                )
                            }
                            is StreamChunk.Thought -> {
                                accumulatedThinking.append(chunk.reasoning)
                                _uiState.value = _uiState.value.copy(
                                    streamingThinking = accumulatedThinking.toString()
                                )
                            }
                            is StreamChunk.Done -> {
                                tokensUsed = chunk.totalTokensUsed
                            }
                            is StreamChunk.Error -> {
                                _uiState.value = _uiState.value.copy(errorMessage = chunk.message)
                                accumulatedContent.append("\n[Core Error: ${chunk.message}]")
                            }
                        }
                    }

                    // 3. Persist Model Message to Local DB
                    val finalContent = accumulatedContent.toString().ifBlank { "[No text generated]" }
                    val finalThinking = accumulatedThinking.toString().takeIf { it.isNotBlank() }

                    val modelMsg = MessageEntity(
                        sessionId = sessId,
                        role = "model",
                        content = finalContent,
                        thinkingProcess = finalThinking,
                        tokenCount = tokensUsed
                    )
                    repository.insertMessage(modelMsg)
                    repository.addSessionTokens(sessId, tokensUsed.toLong().coerceAtLeast(10L))
                } catch (e: Exception) {
                    val errContent = accumulatedContent.toString() + "\n[Stream Interrupted: ${e.message}]"
                    val modelMsg = MessageEntity(
                        sessionId = sessId,
                        role = "model",
                        content = errContent,
                        thinkingProcess = accumulatedThinking.toString().takeIf { it.isNotBlank() }
                    )
                    repository.insertMessage(modelMsg)
                } finally {
                    _uiState.value = _uiState.value.copy(
                        isStreaming = false,
                        streamingContent = "",
                        streamingThinking = ""
                    )
                }
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isStreaming = false
        )
    }

    suspend fun testApiKey(key: String): Result<String> {
        return repository.testApiKey(key)
    }

    fun updateApiKey(key: String) {
        prefs.customApiKey = key
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun updateTemperature(temp: Float) {
        prefs.temperature = temp
        _uiState.value = _uiState.value.copy(temperature = temp)
    }

    fun updateTopP(topP: Float) {
        prefs.topP = topP
        _uiState.value = _uiState.value.copy(topP = topP)
    }

    fun updateTopK(topK: Int) {
        prefs.topK = topK
        _uiState.value = _uiState.value.copy(topK = topK)
    }

    fun updateMaxTokens(tokens: Int) {
        prefs.maxOutputTokens = tokens
        _uiState.value = _uiState.value.copy(maxTokens = tokens)
    }

    fun updateSafetyBlockNone(value: Boolean) {
        prefs.isSafetyBlockNone = value
        _uiState.value = _uiState.value.copy(isSafetyBlockNone = value)
    }

    fun toggleMonetTheme(enabled: Boolean) {
        prefs.isMonetThemeEnabled = enabled
        _uiState.value = _uiState.value.copy(isMonetEnabled = enabled)
    }

    fun selectPersona(personaId: String) {
        prefs.activePersonaId = personaId
        _uiState.value = _uiState.value.copy(activePersonaId = personaId)
    }

    fun addCustomPersona(persona: PersonaEntity) {
        viewModelScope.launch {
            repository.insertPersona(persona)
            selectPersona(persona.id)
        }
    }

    suspend fun exportJson(): String = repository.exportHistoryAsJson()
    suspend fun exportMarkdown(): String = repository.exportHistoryAsMarkdown()

    fun purgeAllData() {
        viewModelScope.launch {
            repository.purgeAllData()
            _uiState.value = KernelUiState(currentScreen = KernelScreen.ONBOARDING)
        }
    }

    private fun serializeAttachments(attachments: List<Attachment>): String {
        val arr = JSONArray()
        for (att in attachments) {
            val obj = JSONObject()
            obj.put("id", att.id)
            obj.put("fileName", att.fileName)
            obj.put("mimeType", att.mimeType)
            obj.put("fileSizeFormatted", att.fileSizeFormatted)
            obj.put("localUri", att.localUri ?: "")
            obj.put("isImage", att.isImage)
            arr.put(obj)
        }
        return arr.toString()
    }
}
