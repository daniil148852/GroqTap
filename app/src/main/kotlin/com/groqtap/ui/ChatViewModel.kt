package com.groqtap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.groqtap.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// ─────────────── State ───────────────

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val streamingMessageId: String? = null,
    val error: String? = null,
    val currentModel: GroqModel = GroqModel.LLAMA_3_3_70B,
)

// ─────────────── ViewModel ───────────────

class ChatViewModel(
    private val prefs: Prefs,
    private val api: GroqApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    init {
        // Observe selected model
        viewModelScope.launch {
            prefs.modelId.collect { id ->
                _uiState.update { it.copy(currentModel = GroqModel.fromId(id)) }
            }
        }
    }

    fun onInputChange(text: String) = _uiState.update { it.copy(inputText = text) }

    fun onSend() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isStreaming) return

        val userMsg = ChatMessage(role = Role.USER, content = text)
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMsg,
                inputText = "",
                error = null,
            )
        }
        startStream()
    }

    private fun startStream() {
        streamJob?.cancel()

        val assistantId = UUID.randomUUID().toString()
        val placeholder = ChatMessage(id = assistantId, role = Role.ASSISTANT, content = "")

        _uiState.update { state ->
            state.copy(
                messages = state.messages + placeholder,
                isStreaming = true,
                streamingMessageId = assistantId,
            )
        }

        streamJob = viewModelScope.launch {
            val apiKey = prefs.apiKey.first()
            val model  = _uiState.value.currentModel
            // Pass all messages except the empty assistant placeholder
            val history = _uiState.value.messages.dropLast(1)

            api.streamChat(apiKey = apiKey, model = model, messages = history)
                .collect { event ->
                    when (event) {
                        is StreamEvent.Token -> appendToken(assistantId, event.text)
                        is StreamEvent.Done  -> finishStream()
                        is StreamEvent.Error -> handleError(assistantId, event.message)
                    }
                }
        }
    }

    private fun appendToken(id: String, token: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == id) msg.copy(content = msg.content + token) else msg
                }
            )
        }
    }

    private fun finishStream() {
        _uiState.update { it.copy(isStreaming = false, streamingMessageId = null) }
    }

    private fun handleError(id: String, error: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.filter { it.id != id },
                isStreaming = false,
                streamingMessageId = null,
                error = error,
            )
        }
    }

    fun clearChat() {
        streamJob?.cancel()
        _uiState.update { it.copy(messages = emptyList(), isStreaming = false, streamingMessageId = null, error = null) }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun stopStreaming() {
        streamJob?.cancel()
        finishStream()
    }

    // ─────────────── Factory ───────────────

    class Factory(private val prefs: Prefs, private val api: GroqApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ChatViewModel(prefs, api) as T
    }
}
