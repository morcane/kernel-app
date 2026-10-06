package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.Attachment
import com.example.data.model.MessageEntity
import com.example.data.model.PersonaEntity
import com.example.data.model.SessionEntity
import com.example.data.network.GeminiClient
import com.example.data.network.StreamChunk
import com.example.data.security.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class KernelRepository(
    private val db: AppDatabase,
    val prefs: PreferencesManager,
    private val geminiClient: GeminiClient = GeminiClient()
) {
    val allSessions: Flow<List<SessionEntity>> = db.sessionDao().getAllSessions()
    val allPersonas: Flow<List<PersonaEntity>> = db.personaDao().getAllPersonas()

    fun getMessagesForSession(sessionId: String): Flow<List<MessageEntity>> =
        db.messageDao().getMessagesForSession(sessionId)

    suspend fun getSession(sessionId: String): SessionEntity? =
        db.sessionDao().getSessionById(sessionId)

    suspend fun createNewSession(
        title: String = "Neural Query",
        modelId: String = prefs.currentModelId,
        personaId: String = prefs.activePersonaId
    ): SessionEntity = withContext(Dispatchers.IO) {
        val session = SessionEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            modelId = modelId,
            personaId = personaId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.sessionDao().insertSession(session)
        session
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) = withContext(Dispatchers.IO) {
        val session = db.sessionDao().getSessionById(sessionId) ?: return@withContext
        db.sessionDao().updateSession(session.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    suspend fun togglePinSession(sessionId: String) = withContext(Dispatchers.IO) {
        val session = db.sessionDao().getSessionById(sessionId) ?: return@withContext
        db.sessionDao().updateSession(session.copy(isPinned = !session.isPinned))
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        db.messageDao().deleteMessagesForSession(sessionId)
        db.sessionDao().deleteSessionById(sessionId)
    }

    suspend fun clearCurrentSessionMessages(sessionId: String) = withContext(Dispatchers.IO) {
        db.messageDao().deleteMessagesForSession(sessionId)
    }

    suspend fun insertMessage(message: MessageEntity) = withContext(Dispatchers.IO) {
        db.messageDao().insertMessage(message)
    }

    suspend fun updateMessage(message: MessageEntity) = withContext(Dispatchers.IO) {
        db.messageDao().updateMessage(message)
    }

    suspend fun addSessionTokens(sessionId: String, tokens: Long) = withContext(Dispatchers.IO) {
        db.sessionDao().addTokens(sessionId, tokens)
    }

    suspend fun searchMessages(query: String): List<MessageEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) emptyList() else db.messageDao().searchMessages(query)
    }

    suspend fun getActivePersonaPrompt(): String = withContext(Dispatchers.IO) {
        val persona = db.personaDao().getPersonaById(prefs.activePersonaId)
        persona?.systemPrompt ?: ""
    }

    suspend fun insertPersona(persona: PersonaEntity) = withContext(Dispatchers.IO) {
        db.personaDao().insertPersona(persona)
    }

    suspend fun deletePersona(persona: PersonaEntity) = withContext(Dispatchers.IO) {
        db.personaDao().deletePersona(persona)
    }

    suspend fun testApiKey(key: String): Result<String> {
        return geminiClient.testApiKey(key)
    }

    fun streamGeminiResponse(
        sessionId: String,
        prompt: String,
        attachments: List<Attachment>,
        history: List<Pair<String, String>>,
        systemPrompt: String
    ): Flow<StreamChunk> {
        return geminiClient.streamGenerateContent(
            apiKey = prefs.getEffectiveApiKey(),
            modelId = prefs.currentModelId,
            systemPrompt = systemPrompt,
            conversationHistory = history,
            currentPrompt = prompt,
            attachments = attachments,
            temperature = prefs.temperature,
            topP = prefs.topP,
            topK = prefs.topK,
            maxTokens = prefs.maxOutputTokens,
            isSafetyBlockNone = prefs.isSafetyBlockNone,
            enableThinking = true
        )
    }

    suspend fun exportHistoryAsJson(): String = withContext(Dispatchers.IO) {
        val sessions = allSessions.first()
        val root = JSONObject()
        root.put("app", "Kernel")
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))

        val sessionsArray = JSONArray()
        for (sess in sessions) {
            val sessObj = JSONObject()
            sessObj.put("id", sess.id)
            sessObj.put("title", sess.title)
            sessObj.put("model", sess.modelId)
            sessObj.put("createdAt", sess.createdAt)

            val msgs = db.messageDao().getMessagesListForSession(sess.id)
            val msgsArray = JSONArray()
            for (m in msgs) {
                val mObj = JSONObject()
                mObj.put("role", m.role)
                mObj.put("content", m.content)
                mObj.put("timestamp", m.timestamp)
                mObj.put("tokens", m.tokenCount)
                msgsArray.put(mObj)
            }
            sessObj.put("messages", msgsArray)
            sessionsArray.put(sessObj)
        }
        root.put("sessions", sessionsArray)
        root.toString(2)
    }

    suspend fun exportHistoryAsMarkdown(): String = withContext(Dispatchers.IO) {
        val sessions = allSessions.first()
        val sb = StringBuilder()
        sb.append("# KERNEL // EXPORTED SESSIONS\n\n")
        sb.append("> Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n\n")

        for (sess in sessions) {
            sb.append("## ${sess.title}\n")
            sb.append("- Model: `${sess.modelId}`\n")
            sb.append("- Total Tokens: ${sess.totalTokens}\n\n")

            val msgs = db.messageDao().getMessagesListForSession(sess.id)
            for (m in msgs) {
                val roleName = if (m.role == "user") "USER" else "KERNEL"
                sb.append("### [$roleName]\n\n")
                if (!m.thinkingProcess.isNullOrBlank()) {
                    sb.append("<details><summary>Thinking Process</summary>\n\n")
                    sb.append(m.thinkingProcess).append("\n\n</details>\n\n")
                }
                sb.append(m.content).append("\n\n---\n\n")
            }
        }
        sb.toString()
    }

    suspend fun purgeAllData() = withContext(Dispatchers.IO) {
        db.messageDao().clearAllMessages()
        db.sessionDao().clearAllSessions()
        prefs.clearAllPreferences()
    }

    suspend fun getStats(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val sessionCount = allSessions.first().size
        val msgCount = db.messageDao().getTotalMessageCount()
        Pair(sessionCount, msgCount)
    }
}
