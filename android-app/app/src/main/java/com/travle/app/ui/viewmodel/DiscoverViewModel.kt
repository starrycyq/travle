package com.travle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travle.app.data.model.CommunityPost
import com.travle.app.data.model.PublishRequest
import com.travle.app.data.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val posts: List<CommunityPost> = emptyList(),
    val isLoading: Boolean = false,
    val publishModalOpen: Boolean = false,
    val publishContent: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: TravelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadCommunityPosts()
    }

    fun loadCommunityPosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getCommunityList()
                .onSuccess { posts ->
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        isLoading = false
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "加载失败"
                    )
                }
        }
    }

    fun togglePublishModal(open: Boolean) {
        _uiState.value = _uiState.value.copy(publishModalOpen = open)
    }

    fun updatePublishContent(content: String) {
        _uiState.value = _uiState.value.copy(publishContent = content)
    }

    fun publishPost() {
        viewModelScope.launch {
            val content = _uiState.value.publishContent
            if (content.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val request = PublishRequest(content = content)
                repository.publishPost(request)
                    .onSuccess {
                        loadCommunityPosts() // 刷新列表
                        _uiState.value = _uiState.value.copy(
                            publishModalOpen = false,
                            publishContent = "",
                            isLoading = false,
                            successMessage = "发布成功！"
                        )
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "发布失败"
                        )
                    }
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "请输入内容"
                )
            }
        }
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            repository.likePost(postId)
                .onSuccess {
                    // 更新本地帖子点赞状态
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(like_count = post.like_count + 1)
                        } else post
                    }
                    _uiState.value = _uiState.value.copy(posts = updatedPosts)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.message ?: "点赞失败"
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}