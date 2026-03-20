package com.groqtap.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// ──────────────────────────── Domain ────────────────────────────

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class Role(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system")
}

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

// ──────────────────────────── Groq models ────────────────────────────

enum class GroqModel(
    val id: String,
    val displayName: String,
    val contextWindow: String,
    val speed: String,
) {
    LLAMA_3_3_70B(
        "llama-3.3-70b-versatile",
        "Llama 3.3 70B",
        "128K",
        "Fast"
    ),
    LLAMA_3_1_8B(
        "llama-3.1-8b-instant",
        "Llama 3.1 8B Instant",
        "128K",
        "Ultra-fast"
    ),
    LLAMA_3_70B(
        "llama3-70b-8192",
        "Llama 3 70B",
        "8K",
        "Fast"
    ),
    GEMMA2_9B(
        "gemma2-9b-it",
        "Gemma 2 9B",
        "8K",
        "Fast"
    ),
    MIXTRAL_8X7B(
        "mixtral-8x7b-32768",
        "Mixtral 8x7B",
        "32K",
        "Balanced"
    );

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: LLAMA_3_3_70B
    }
}

// ──────────────────────────── API wire types ────────────────────────────

@Serializable
data class ApiRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 2048,
)

@Serializable
data class ApiMessage(
    val role: String,
    val content: String,
)

@Serializable
data class StreamChunk(
    val id: String = "",
    val choices: List<StreamChoice> = emptyList(),
)

@Serializable
data class StreamChoice(
    val delta: Delta = Delta(),
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class Delta(
    val content: String? = null,
    val role: String? = null,
)

@Serializable
data class ErrorResponse(
    val error: ApiError,
)

@Serializable
data class ApiError(
    val message: String,
    val type: String = "",
    val code: String? = null,
)
