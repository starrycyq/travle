package com.travle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travle.app.data.database.UserPreference
import com.travle.app.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreferencesUiState(
    val destination: String = "",
    val preferences: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val userPreferences: List<UserPreference> = emptyList(),
    val isHistoryVisible: Boolean = false
)

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    init {
        observeUserPreferences()
    }

    private fun observeUserPreferences() {
        viewModelScope.launch {
            preferencesRepository.getUserPreferences()
                .catch { e -> 
                    _uiState.update { state -> state.copy(errorMessage = e.message) }
                }
                .collect { preferences ->
                    _uiState.update { state -> state.copy(userPreferences = preferences) }
                }
        }
    }

    fun updateDestination(destination: String) {
        _uiState.value = _uiState.value.copy(destination = destination)
    }

    fun updatePreferences(preferences: String) {
        _uiState.value = _uiState.value.copy(preferences = preferences)
    }

    fun saveUserPreference() {
        viewModelScope.launch {
            if (_uiState.value.destination.isBlank() || _uiState.value.preferences.isBlank()) {
                _uiState.update { state -> 
                    state.copy(errorMessage = "请输入目的地和偏好") 
                }
                return@launch
            }
            
            _uiState.update { state -> state.copy(isLoading = true, errorMessage = null, successMessage = null) }

            preferencesRepository.saveUserPreference(
                _uiState.value.destination,
                _uiState.value.preferences
            )
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            successMessage = "偏好已保存",
                            destination = "",
                            preferences = ""
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "保存失败"
                        )
                    }
                }
        }
    }

    fun toggleHistoryVisibility() {
        _uiState.update { state -> 
            state.copy(isHistoryVisible = !state.isHistoryVisible) 
        }
    }

    fun loadUserPreferences() {
        viewModelScope.launch {
            preferencesRepository.getUserPreferences()
                .catch { e -> 
                    _uiState.update { state -> state.copy(errorMessage = e.message) }
                }
                .collect { preferences ->
                    _uiState.update { state -> state.copy(userPreferences = preferences) }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { state -> 
            state.copy(errorMessage = null, successMessage = null) 
        }
    }

    fun clearForm() {
        _uiState.update { state -> 
            state.copy(destination = "", preferences = "") 
        }
    }

    fun selectHistoryPreference(userPreference: UserPreference) {
        _uiState.update { state ->
            state.copy(
                destination = userPreference.destination,
                preferences = userPreference.preferences,
                isHistoryVisible = false
            )
        }
    }
}