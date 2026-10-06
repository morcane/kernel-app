package com.example.data.network

import com.example.data.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

sealed class StreamChunk {
    data class Text(val content: String) : StreamChunk()
    data class Thought(val reasoning: String) : StreamChunk()
    data class Done(val totalTokensUsed: Int) : StreamChunk()
    data class Error(val message: String) : StreamChunk()
}

class GeminiClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key is empty"))
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey.trim()}"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("200 OK • Core Online")
                } else {
                    val code = response.code
                    val body = response.body?.string().orEmpty()
                    val msg = try {
                        val json = JSONObject(body)
                        json.optJSONObject("error")?.optString("message") ?: "HTTP $code"
                    } catch (_: Exception) {
                        "HTTP $code: ${response.message}"
                    }
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamGenerateContent(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>>, // role to text
        currentPrompt: String,
        attachments: List<Attachment>,
        temperature: Float,
        topP: Float,
        topK: Int,
        maxTokens: Int,
        isSafetyBlockNone: Boolean,
        enableThinking: Boolean = true
    ): Flow<StreamChunk> = callbackFlow {
        val rootJson = JSONObject()

        // System Instruction
        if (systemPrompt.isNotBlank()) {
            val sysObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", systemPrompt))
            sysObj.put("parts", sysParts)
            rootJson.put("systemInstruction", sysObj)
        }

        // Contents (Conversation History + Current Prompt)
        val contentsArray = JSONArray()

        // Add history turns (limit to last 20 to avoid context overflow)
        val trimmedHistory = conversationHistory.takeLast(20)
        for ((role, text) in trimmedHistory) {
            val contentObj = JSONObject()
            contentObj.put("role", if (role == "user") "user" else "model")
            val parts = JSONArray()
            parts.put(JSONObject().put("text", text))
            contentObj.put("parts", parts)
            contentsArray.put(contentObj)
        }

        // Current user message turn
        val currentUserContent = JSONObject()
        currentUserContent.put("role", "user")
        val currentParts = JSONArray()

        // Add text prompt
        if (currentPrompt.isNotBlank()) {
            currentParts.put(JSONObject().put("text", currentPrompt))
        }

        // Add attachments (Images as inlineData, text/code as text attachments)
        for (att in attachments) {
            if (att.isImage && !att.base64Data.isNullOrBlank()) {
                val inlineData = JSONObject()
                inlineData.put("mimeType", att.mimeType)
                inlineData.put("data", att.base64Data)
                currentParts.put(JSONObject().put("inlineData", inlineData))
            } else if (!att.base64Data.isNullOrBlank()) {
                // Code or document text
                val fileHeader = "\n[File: ${att.fileName} (${att.mimeType})]\n${att.base64Data}\n"
                currentParts.put(JSONObject().put("text", fileHeader))
            }
        }
        currentUserContent.put("parts", currentParts)
        contentsArray.put(currentUserContent)
        rootJson.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", temperature.toDouble())
        genConfig.put("topP", topP.toDouble())
        genConfig.put("topK", topK)
        genConfig.put("maxOutputTokens", maxTokens)

        if (enableThinking) {
            val thinkingConfig = JSONObject()
            thinkingConfig.put("thinkingLevel", "low")
            genConfig.put("thinkingConfig", thinkingConfig)
        }
        rootJson.put("generationConfig", genConfig)

        // Safety Settings
        if (isSafetyBlockNone) {
            val safetyArray = JSONArray()
            val categories = listOf(
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT"
            )
            for (cat in categories) {
                safetyArray.put(
                    JSONObject()
                        .put("category", cat)
                        .put("threshold", "BLOCK_NONE")
                )
            }
            rootJson.put("safetySettings", safetyArray)
        }

        val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${modelId}:streamGenerateContent?alt=sse&key=${apiKey.trim()}"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val call = client.newCall(request)

        val thread = Thread {
            var response: Response? = null
            try {
                response = call.execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    val errorMessage = try {
                        val errJson = JSONObject(errorBody)
                        errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }
                    trySend(StreamChunk.Error(errorMessage))
                    close()
                    return@Thread
                }

                val body = response.body
                if (body == null) {
                    trySend(StreamChunk.Error("Empty response body"))
                    close()
                    return@Thread
                }

                val reader = BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8))
                var line: String?
                var totalEstimatedTokens = 0

                while (reader.readLine().also { line = it } != null) {
                    val rawLine = line?.trim() ?: continue
                    if (!rawLine.startsWith("data:")) continue
                    val payload = rawLine.removePrefix("data:").trim()
                    if (payload.isEmpty() || payload == "[DONE]") continue

                    try {
                        val chunkObj = JSONObject(payload)
                        val candidates = chunkObj.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            if (content != null) {
                                val parts = content.optJSONArray("parts")
                                if (parts != null) {
                                    for (i in 0 until parts.length()) {
                                        val part = parts.getJSONObject(i)
                                        val isThought = part.optBoolean("thought", false)
                                        val text = part.optString("text", "")
                                        if (text.isNotEmpty()) {
                                            if (isThought) {
                                                trySend(StreamChunk.Thought(text))
                                            } else {
                                                trySend(StreamChunk.Text(text))
                                            }
                                            totalEstimatedTokens += (text.length / 4).coerceAtLeast(1)
                                        }
                                    }
                                }
                            }
                        }

                        // Check usage metadata if provided
                        val usage = chunkObj.optJSONObject("usageMetadata")
                        if (usage != null) {
                            val candidatesTokens = usage.optInt("candidatesTokenCount", 0)
                            if (candidatesTokens > 0) {
                                totalEstimatedTokens = candidatesTokens
                            }
                        }
                    } catch (pe: Exception) {
                        // ignore malformed SSE chunk line
                    }
                }

                trySend(StreamChunk.Done(totalEstimatedTokens))
                close()
            } catch (e: Exception) {
                if (!call.isCanceled()) {
                    trySend(StreamChunk.Error(e.message ?: "Connection error"))
                }
                close()
            } finally {
                response?.close()
            }
        }

        thread.start()

        awaitClose {
            call.cancel()
            thread.interrupt()
        }
    }
}
