package com.travle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travle.app.data.database.CollectionItem
import com.travle.app.data.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuideDetailUiState(
    val destination: String = "",
    val preferences: String = "",
    val guide: String = "",
    val images: List<String> = emptyList(),
    val isCollected: Boolean = false,
    val imageModalOpen: Boolean = false,
    val selectedImageIndex: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class GuideDetailViewModel @Inject constructor(
    private val repository: TravelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuideDetailUiState())
    val uiState: StateFlow<GuideDetailUiState> = _uiState.asStateFlow()

    fun toggleCollection() {
        _uiState.value = _uiState.value.copy(
            isCollected = !_uiState.value.isCollected,
            successMessage = if (!_uiState.value.isCollected) "收藏成功" else "已取消收藏"
        )
    }

    fun toggleImageModal(open: Boolean) {
        _uiState.value = _uiState.value.copy(imageModalOpen = open)
    }

    fun updateSelectedImageIndex(index: Int) {
        _uiState.value = _uiState.value.copy(selectedImageIndex = index)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun updateGuideData(destination: String, preferences: String, guide: String, images: List<String>) {
        _uiState.value = _uiState.value.copy(
            destination = destination,
            preferences = preferences,
            guide = guide,
            images = images
        )
    }
}