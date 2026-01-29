package com.travle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travle.app.data.model.GenerateGuideRequest
import com.travle.app.data.model.GuideResult
import com.travle.app.data.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val destination: String = "",
    val preferences: String = "",
    val guide: String? = null,
    val images: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val shareModalOpen: Boolean = false,
    val shareContent: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TravelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun updateDestination(destination: String) {
        _uiState.value = _uiState.value.copy(destination = destination)
    }

    fun updatePreferences(preferences: String) {
        _uiState.value = _uiState.value.copy(preferences = preferences)
    }

    fun generateGuide() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            val request = GenerateGuideRequest(
                destination = _uiState.value.destination,
                preferences = _uiState.value.preferences
            )

            repository.generateGuide(request)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        guide = result.guide,
                        images = result.images ?: emptyList(),
                        successMessage = "攻略生成成功！"
                    )
                    
                    // 同时保存用户偏好
                    saveUserPreference()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "生成攻略失败"
                    )
                }
        }
    }

    private fun saveUserPreference() {
        viewModelScope.launch {
            val request = com.travle.app.data.model.InputPreferenceRequest(
                destination = _uiState.value.destination,
                preferences = _uiState.value.preferences
            )
            
            repository.inputPreference(request)
                .onFailure { exception ->
                    // 记录错误但不显示给用户，因为这只是辅助功能
                    println("保存用户偏好失败: ${exception.message}")
                }
        }
    }

    fun clearInputs() {
        _uiState.value = _uiState.value.copy(
            destination = "",
            preferences = "",
            guide = null,
            images = emptyList()
        )
    }

    fun toggleShareModal(open: Boolean) {
        _uiState.value = _uiState.value.copy(shareModalOpen = open)
    }

    fun updateShareContent(content: String) {
        _uiState.value = _uiState.value.copy(shareContent = content)
    }

    fun publishGuide() {
        viewModelScope.launch {
            val shareContent = _uiState.value.shareContent
            if (shareContent.isNotEmpty()) {
                // 调用上传API
                val request = com.travle.app.data.model.UploadGuideRequest(
                    text = shareContent,
                    images = _uiState.value.images
                )
                repository.uploadGuide(request)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            shareModalOpen = false,
                            shareContent = "",
                            successMessage = "攻略发布成功！"
                        )
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = exception.message ?: "发布失败"
                        )
                    }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun navigateToGuideDetail() {
        // 触发导航到详情页面的逻辑
        // 可以在这里更新状态，让UI层监听变化并执行导航
    }
}