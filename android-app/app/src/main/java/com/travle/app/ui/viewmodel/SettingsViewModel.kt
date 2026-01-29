package com.travle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travle.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "light",
    val aboutModalOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            preferencesRepository.theme.collect { theme ->
                _uiState.value = _uiState.value.copy(theme = theme)
            }
        }
    }

    suspend fun toggleTheme() {
        val newTheme = if (uiState.value.theme == "light") "dark" else "light"
        preferencesRepository.setTheme(newTheme)
    }

    fun clearCache() {
        // 模拟清除缓存
        viewModelScope.launch {
            // 这里可以添加实际的缓存清除逻辑
        }
    }

    fun toggleAboutModal(open: Boolean) {
        _uiState.value = _uiState.value.copy(aboutModalOpen = open)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}