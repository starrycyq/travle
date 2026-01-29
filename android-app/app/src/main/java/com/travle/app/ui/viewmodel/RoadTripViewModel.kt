package com.travle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travle.app.data.model.RoadTripResult
import com.travle.app.data.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoadTripUiState(
    val start: String = "",
    val destination: String = "",
    val preferences: String = "",
    val routeType: String = "fastest",
    val route: RoadTripResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val shareModalOpen: Boolean = false,
    val shareContent: String = ""
)

@HiltViewModel
class RoadTripViewModel @Inject constructor(
    private val repository: TravelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoadTripUiState())
    val uiState: StateFlow<RoadTripUiState> = _uiState.asStateFlow()

    fun updateStart(start: String) {
        _uiState.value = _uiState.value.copy(start = start)
    }

    fun updateDestination(destination: String) {
        _uiState.value = _uiState.value.copy(destination = destination)
    }

    fun updatePreferences(preferences: String) {
        _uiState.value = _uiState.value.copy(preferences = preferences)
    }

    fun updateRouteType(routeType: String) {
        _uiState.value = _uiState.value.copy(routeType = routeType)
    }

    fun generateRoadTrip() {
        val start = _uiState.value.start.trim()
        val destination = _uiState.value.destination.trim()
        
        if (start.isEmpty() || destination.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "请输入起点和目的地"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            repository.generateRoadTrip(
                start = start,
                destination = destination,
                preferences = _uiState.value.preferences,
                routeType = _uiState.value.routeType
            )
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        route = result,
                        successMessage = "自驾路线生成成功！"
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "生成自驾路线失败"
                    )
                }
        }
    }

    fun clearInputs() {
        _uiState.value = _uiState.value.copy(
            start = "",
            destination = "",
            preferences = "",
            route = null
        )
    }

    fun toggleShareModal(open: Boolean) {
        _uiState.value = _uiState.value.copy(shareModalOpen = open)
    }

    fun updateShareContent(content: String) {
        _uiState.value = _uiState.value.copy(shareContent = content)
    }

    fun publishRoadTrip() {
        viewModelScope.launch {
            val shareContent = _uiState.value.shareContent
            if (shareContent.isNotEmpty()) {
                // TODO: 调用上传API发布自驾攻略
                // 暂时模拟成功
                _uiState.value = _uiState.value.copy(
                    shareModalOpen = false,
                    shareContent = "",
                    successMessage = "自驾攻略发布成功！"
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