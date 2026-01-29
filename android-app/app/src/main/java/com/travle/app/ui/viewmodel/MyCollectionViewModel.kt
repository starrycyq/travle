package com.travle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travle.app.data.database.CollectionItem
import com.travle.app.data.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyCollectionUiState(
    val collections: List<CollectionItem> = emptyList(),
    val isLoading: Boolean = false,
    val clearModalOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class MyCollectionViewModel @Inject constructor(
    private val repository: TravelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyCollectionUiState(isLoading = true))
    val uiState: StateFlow<MyCollectionUiState> = _uiState.asStateFlow()

    init {
        loadCollections()
    }

    private fun loadCollections() {
        viewModelScope.launch {
            repository.getAllCollections().collect { collections ->
                _uiState.value = _uiState.value.copy(
                    collections = collections,
                    isLoading = false
                )
            }
        }
    }

    fun toggleClearModal(open: Boolean) {
        _uiState.value = _uiState.value.copy(clearModalOpen = open)
    }

    fun clearAllCollections() {
        viewModelScope.launch {
            repository.clearAllCollections()
            _uiState.value = _uiState.value.copy(
                collections = emptyList(),
                successMessage = "已清空所有收藏"
            )
        }
    }

    fun deleteCollection(item: CollectionItem) {
        viewModelScope.launch {
            repository.deleteCollection(item)
            _uiState.value = _uiState.value.copy(
                successMessage = "已删除收藏"
            )
        }
    }

    fun addCollection(item: CollectionItem) {
        viewModelScope.launch {
            repository.addCollection(item)
            _uiState.value = _uiState.value.copy(
                successMessage = "已添加收藏"
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun navigateToGuideDetail(collection: CollectionItem) {
        // 这里可以触发导航事件，实际导航会在UI层处理
        // 或者可以在这里更新UI状态，让UI层监听变化并执行导航
        // 使用参数以避免警告
        println(collection.id)
    }
}