package com.groqtap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed class StreamEvent {
    data class Token(val text: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
    object Done : StreamEvent()
}

class GroqApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /** Returns a cold Flow of StreamEvents from Groq's streaming endpoint. */
    fun streamChat(
        apiKey: String,
        model: GroqModel,
        messages: List<ChatMessage>,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    ): Flow<StreamEvent> = flow {
        if (apiKey.isBlank()) {
            emit(StreamEvent.Error("API key not set. Go to Settings to add your Groq key."))
            return@flow
        }

        val apiMessages = buildList {
            add(ApiMessage(role = "system", content = systemPrompt))
            messages.forEach { add(ApiMessage(role = it.role.value, content = it.content)) }
        }

        val requestBody = json.encodeToString(
            ApiRequest.serializer(),
            ApiRequest(model = model.id, messages = apiMessages)
        )

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .post(requestBody.toRequestBody(mediaType))
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    val errorMsg = try {
                        json.decodeFromString(ErrorResponse.serializer(), errorBody).error.message
                    } catch (_: Exception) {
                        "HTTP ${response.code}: $errorBody"
                    }
                    emit(StreamEvent.Error(errorMsg))
                    return@use
                }

                val reader = response.body?.charStream()?.buffered()
                    ?: run { emit(StreamEvent.Error("Empty response body")); return@use }

                reader.use { br ->
                    br.lineSequence().forEach { line ->
                        if (!line.startsWith("data: ")) return@forEach
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") {
                            emit(StreamEvent.Done)
                            return@use
                        }
                        try {
                            val chunk = json.decodeFromString(StreamChunk.serializer(), data)
                            val token = chunk.choices.firstOrNull()?.delta?.content
                            if (!token.isNullOrEmpty()) emit(StreamEvent.Token(token))
                        } catch (_: Exception) { /* skip malformed SSE chunks */ }
                    }
                }
            }
        } catch (e: Exception) {
            emit(StreamEvent.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """You are GroqTap — a fast, intelligent AI assistant powered by Groq's inference infrastructure. You are concise, helpful, and direct. You format your responses clearly using markdown when appropriate."""
    }
}
