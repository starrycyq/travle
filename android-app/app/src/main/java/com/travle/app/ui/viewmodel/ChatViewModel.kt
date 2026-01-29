package com.travle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travle.app.data.auth.AuthManager
import com.travle.app.data.auth.AuthStatus
import com.travle.app.data.model.ChatMessage
import com.travle.app.data.model.ChatResponse
import com.travle.app.data.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputMessage: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val conversationId: String? = null,
    val isLoggedIn: Boolean = false,
    val showLoginPrompt: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    init {
        // 监听认证状态变化
        authManager.authStatus.onEach { authStatus ->
            val isLoggedIn = authStatus == AuthStatus.AUTHENTICATED
            _uiState.value = _uiState.value.copy(
                isLoggedIn = isLoggedIn,
                showLoginPrompt = !isLoggedIn && _uiState.value.messages.isEmpty()
            )
            
            // 如果用户登录了，加载聊天历史
            if (isLoggedIn) {
                loadChatHistory()
            }
        }.launchIn(viewModelScope)
    }

    fun updateInputMessage(message: String) {
        _uiState.value = _uiState.value.copy(inputMessage = message)
    }

    fun sendMessage() {
        val message = _uiState.value.inputMessage.trim()
        if (message.isEmpty()) return
        
        // 如果用户未登录，显示登录提示
        if (!_uiState.value.isLoggedIn) {
            _uiState.value = _uiState.value.copy(showLoginPrompt = true)
            return
        }
        
        viewModelScope.launch {
            // 添加用户消息到列表
            val userMessage = ChatMessage(
                id = "user_${System.currentTimeMillis()}",
                role = "user",
                content = message
            )
            
            val updatedMessages = _uiState.value.messages + userMessage
            _uiState.value = _uiState.value.copy(
                messages = updatedMessages,
                inputMessage = "",
                isSending = true,
                errorMessage = null
            )
            
            // 发送到服务器
            repository.sendChatMessage(message, _uiState.value.conversationId)
                .onSuccess { response ->
                    // 添加AI回复
                    response.data?.let { data ->
                        val aiMessage = ChatMessage(
                            id = "ai_${System.currentTimeMillis()}",
                            role = "assistant",
                            content = data.response
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            messages = updatedMessages + aiMessage,
                            isSending = false,
                            conversationId = data.conversation_id ?: _uiState.value.conversationId
                        )
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            errorMessage = "收到空回复"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        errorMessage = exception.message ?: "发送消息失败"
                    )
                }
        }
    }

    fun loadChatHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            repository.getChatHistory(_uiState.value.conversationId)
                .onSuccess { response ->
                    response.data?.let { data ->
                        val chatMessages = data.messages.map { msg ->
                            ChatMessage(
                                id = "${msg.role}_${msg.timestamp}",
                                role = msg.role,
                                content = msg.content,
                                timestamp = msg.timestamp
                            )
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            messages = chatMessages,
                            isLoading = false,
                            conversationId = data.conversation_id ?: _uiState.value.conversationId
                        )
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "加载历史失败"
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "加载历史失败"
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            conversationId = null,
            errorMessage = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun dismissLoginPrompt() {
        _uiState.value = _uiState.value.copy(showLoginPrompt = false)
    }

    fun login() {
        // 触发登录流程
        _uiState.value = _uiState.value.copy(showLoginPrompt = false)
        // 注意：实际登录由ProfileViewModel处理，这里只是关闭提示
    }
}