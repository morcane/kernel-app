package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "New Session",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String = "gemini-2.5-flash",
    val isPinned: Boolean = false,
    val personaId: String = "default_core",
    val totalTokens: Long = 0L
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: String, // "user", "model", "system"
    val content: String,
    val thinkingProcess: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tokenCount: Int = 0,
    val attachmentsJson: String = "[]" // Serialized list of Attachment
)

@Entity(tableName = "personas")
data class PersonaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tag: String,
    val description: String,
    val systemPrompt: String,
    val isCustom: Boolean = false
)

data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val mimeType: String,
    val fileSizeFormatted: String,
    val base64Data: String? = null,
    val localUri: String? = null,
    val lineCount: Int? = null,
    val isImage: Boolean = false
)

data class ModelInfo(
    val id: String,
    val displayName: String,
    val contextWindow: String,
    val description: String,
    val badge: String
)

val AVAILABLE_MODELS = listOf(
    ModelInfo(
        id = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        contextWindow = "1M tokens",
        description = "Ultra-fast multimodal core for real-time computing and code generation",
        badge = "FAST"
    ),
    ModelInfo(
        id = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        contextWindow = "2M tokens",
        description = "Maximum reasoning capacity, complex STEM analysis and deep architecture",
        badge = "PRO"
    ),
    ModelInfo(
        id = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        contextWindow = "1M tokens",
        description = "Next-gen balanced speed and frontier intelligence",
        badge = "3.5"
    ),
    ModelInfo(
        id = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro",
        contextWindow = "2M tokens",
        description = "Advanced logical reasoning, math proofs and full-repo analysis",
        badge = "STEM"
    )
)

val DEFAULT_PERSONAS = listOf(
    PersonaEntity(
        id = "default_core",
        name = "Default Core",
        tag = "STRICT",
        description = "Strict, precise, uncompromisingly technical. Zero conversational fluff.",
        systemPrompt = "You are Kernel, a high-performance local-first neural computing core. Provide direct, rigorous, uncompromisingly precise technical answers. Avoid conversational filler, greetings, apologies, or disclaimers. Prioritize clear reasoning, compact code, and factual depth."
    ),
    PersonaEntity(
        id = "polyglot_dev",
        name = "Senior Polyglot Dev",
        tag = "CODE",
        description = "Production-grade code, architectural patterns, clean idiomatic idioms.",
        systemPrompt = "You are a Principal Software Architect and Polyglot Developer. Respond exclusively with production-ready, strictly typed code and brief architectural notes. Follow SOLID principles, zero unnecessary dependencies, and idiomatic patterns."
    ),
    PersonaEntity(
        id = "reverse_engineer",
        name = "Reverse Engineer",
        tag = "SEC",
        description = "Bytecode, assembly, decompilation, binary security vectors.",
        systemPrompt = "You are a Low-Level Systems and Binary Exploitation Specialist. Focus on memory models, CPU registers, assembly (x86_64/ARM64), decompiled bytecode, kernel interfaces, and security vulnerability patterns."
    ),
    PersonaEntity(
        id = "minimalist_analyst",
        name = "Minimalist Analyst",
        tag = "LOGIC",
        description = "Quantitative density, structured bullet points, decisive conclusions.",
        systemPrompt = "You are a Minimalist Systems Analyst. Distill complex topics into high-density structured tables, quantitative trade-offs, and definitive actionable conclusions. Zero pleasantries."
    )
)
