package com.travle.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travle.app.data.auth.AuthManager
import com.travle.app.data.auth.AuthStatus
import com.travle.app.data.repository.PreferencesRepository
import com.travle.app.data.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

data class ProfileUiState(
    val nickname: String = "旅行爱好者",
    val theme: String = "light",
    val collectionsCount: Int = 12,
    val sharesCount: Int = 8,
    val likesCount: Int = 156,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showLoginDialog: Boolean = false,
    val showXiaohongshuDialog: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val authManager: AuthManager,
    private val travelRepository: TravelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        // 监听认证状态变化
        authManager.authStatus.onEach { authStatus ->
            _uiState.value = _uiState.value.copy(
                isLoggedIn = authStatus == AuthStatus.AUTHENTICATED
            )
        }.launchIn(viewModelScope)
        
        // 监听昵称变化
        viewModelScope.launch {
            preferencesRepository.nickname.collect { nickname ->
                _uiState.value = _uiState.value.copy(nickname = nickname)
            }
        }
        
        // 加载用户统计数据（如果已登录）
        loadUserStats()
    }
    
    private fun loadUserStats() {
        viewModelScope.launch {
            if (authManager.isLoggedIn()) {
                // TODO: 从API获取真实统计数据
                // 暂时使用硬编码数据
                _uiState.value = _uiState.value.copy(
                    collectionsCount = 12,
                    sharesCount = 8,
                    likesCount = 156
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    collectionsCount = 0,
                    sharesCount = 0,
                    likesCount = 0
                )
            }
        }
    }
    
    fun toggleLoginDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLoginDialog = show)
    }
    
    fun toggleXiaohongshuDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showXiaohongshuDialog = show)
    }
    
    suspend fun login(username: String, password: String): Boolean {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        return try {
            // 调用API登录
            val loginRequest = com.travle.app.data.model.LoginRequest(
                username = username,
                password = password
            )
            
            val result = travelRepository.login(loginRequest)
            
            if (result.isSuccess) {
                val loginResult = result.getOrThrow()
                
                // 检查登录结果
                if (loginResult.status != "success" || loginResult.data == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = loginResult.message ?: "登录失败"
                    )
                    return false
                }
                
                val loginData = loginResult.data!!
                val userInfo = loginData.user_info
                
                // 保存认证信息到AuthManager
                val tokenInfo = com.travle.app.data.auth.TokenInfo(
                    accessToken = loginData.access_token,
                    tokenType = loginData.token_type,
                    expiresIn = loginData.expires_in
                )
                
                val authUser = com.travle.app.data.auth.AuthUser(
                    userId = userInfo.user_id,
                    username = userInfo.username,
                    email = userInfo.email,
                    role = userInfo.role
                )
                
                authManager.saveLoginInfo(tokenInfo, authUser)
                
                // 更新昵称
                preferencesRepository.setNickname(userInfo.username)
                
                // 加载用户统计数据
                loadUserStats()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showLoginDialog = false,
                    isLoggedIn = true,
                    nickname = userInfo.username
                )
                true
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "登录失败"
                )
                false
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = e.message ?: "登录失败"
            )
            false
        }
    }
    
    suspend fun logout() {
        viewModelScope.launch {
            authManager.logout()
            _uiState.value = _uiState.value.copy(isLoggedIn = false)
            loadUserStats()
        }
    }
    
    suspend fun updateNickname(nickname: String) {
        preferencesRepository.setNickname(nickname)
    }

    suspend fun updateTheme(theme: String) {
        preferencesRepository.setTheme(theme)
    }
    
    suspend fun authenticateXiaohongshu(phoneNumber: String, verificationCode: String): Boolean {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        return try {
            // TODO: 调用后端API进行小红书授权
            // 暂时模拟成功
            delay(1000) // 模拟网络延迟
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showXiaohongshuDialog = false,
                successMessage = "小红书授权成功！现在可以爬取小红书内容了。"
            )
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = e.message ?: "授权失败，请重试"
            )
            false
        }
    }
    
    suspend fun sendXiaohongshuVerificationCode(phoneNumber: String): Boolean {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        return try {
            // TODO: 调用后端API发送验证码
            delay(1000) // 模拟网络延迟
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                successMessage = "验证码已发送到 $phoneNumber"
            )
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = e.message ?: "发送验证码失败"
            )
            false
        }
    }
    
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}