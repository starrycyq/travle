package com.travle.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证状态枚举
 */
enum class AuthStatus {
    UNAUTHENTICATED,    // 未认证
    AUTHENTICATED,      // 已认证
    EXPIRED,           // Token过期
    ERROR               // 认证错误
}

/**
 * 用户信息数据类
 */
data class AuthUser(
    val userId: String,
    val username: String,
    val email: String,
    val role: String
)

/**
 * Token信息数据类
 */
data class TokenInfo(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val issuedAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        val expirationTime = issuedAt + (expiresIn * 1000L)
        return System.currentTimeMillis() >= expirationTime - 60000 // 提前1分钟过期
    }
}

/**
 * 认证管理器
 * 管理用户登录状态、Token存储和刷新
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // 认证状态流
    private val _authStatus = MutableStateFlow<AuthStatus>(AuthStatus.UNAUTHENTICATED)
    val authStatus: Flow<AuthStatus> = _authStatus.asStateFlow()
    
    // 当前用户流
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: Flow<AuthUser?> = _currentUser.asStateFlow()
    
    // Token信息流
    private val _tokenInfo = MutableStateFlow<TokenInfo?>(null)
    val tokenInfo: Flow<TokenInfo?> = _tokenInfo.asStateFlow()
    
    companion object {
        private const val KEY_TOKEN_INFO = "token_info"
        private const val KEY_USER_INFO = "user_info"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    init {
        loadAuthData()
    }
    
    /**
     * 保存登录信息
     */
    suspend fun saveLoginInfo(
        tokenInfo: TokenInfo,
        userInfo: AuthUser
    ) {
        try {
            // 保存到SharedPreferences
            prefs.edit().apply {
                putString(KEY_TOKEN_INFO, gson.toJson(tokenInfo))
                putString(KEY_USER_INFO, gson.toJson(userInfo))
                putBoolean(KEY_IS_LOGGED_IN, true)
            }.apply()
            
            // 更新状态流
            _tokenInfo.value = tokenInfo
            _currentUser.value = userInfo
            _authStatus.value = AuthStatus.AUTHENTICATED
            
        } catch (e: Exception) {
            _authStatus.value = AuthStatus.ERROR
            throw e
        }
    }
    
    /**
     * 获取当前Token
     */
    fun getCurrentToken(): String? {
        return _tokenInfo.value?.accessToken
    }
    
    /**
     * 获取当前用户
     */
    fun getCurrentUser(): AuthUser? {
        return _currentUser.value
    }
    
    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        return _authStatus.value == AuthStatus.AUTHENTICATED && 
               !(_tokenInfo.value?.isExpired() ?: true)
    }
    
    /**
     * 检查Token是否过期
     */
    fun isTokenExpired(): Boolean {
        return _tokenInfo.value?.isExpired() ?: true
    }
    
    /**
     * 更新Token（用于刷新）
     */
    suspend fun updateToken(newTokenInfo: TokenInfo) {
        try {
            val currentUser = _currentUser.value ?: throw Exception("用户信息不存在")
            
            prefs.edit().apply {
                putString(KEY_TOKEN_INFO, gson.toJson(newTokenInfo))
            }.apply()
            
            _tokenInfo.value = newTokenInfo
            _authStatus.value = AuthStatus.AUTHENTICATED
            
        } catch (e: Exception) {
            _authStatus.value = AuthStatus.ERROR
            throw e
        }
    }
    
    /**
     * 登出
     */
    suspend fun logout() {
        try {
            prefs.edit().apply {
                remove(KEY_TOKEN_INFO)
                remove(KEY_USER_INFO)
                remove(KEY_IS_LOGGED_IN)
            }.apply()
            
            _tokenInfo.value = null
            _currentUser.value = null
            _authStatus.value = AuthStatus.UNAUTHENTICATED
            
        } catch (e: Exception) {
            _authStatus.value = AuthStatus.ERROR
            throw e
        }
    }
    
    /**
     * 检查Token状态并更新认证状态
     */
    suspend fun checkTokenStatus() {
        try {
            val tokenInfo = _tokenInfo.value
            val isExpired = tokenInfo?.isExpired() ?: true
            val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            
            when {
                !isLoggedIn || tokenInfo == null -> {
                    _authStatus.value = AuthStatus.UNAUTHENTICATED
                }
                isExpired -> {
                    _authStatus.value = AuthStatus.EXPIRED
                }
                else -> {
                    _authStatus.value = AuthStatus.AUTHENTICATED
                }
            }
        } catch (e: Exception) {
            _authStatus.value = AuthStatus.ERROR
        }
    }
    
    /**
     * 从本地存储加载认证数据
     */
    private fun loadAuthData() {
        try {
            val isLogged = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            if (!isLogged) {
                _authStatus.value = AuthStatus.UNAUTHENTICATED
                return
            }
            
            val tokenInfoJson = prefs.getString(KEY_TOKEN_INFO, null)
            val userInfoJson = prefs.getString(KEY_USER_INFO, null)
            
            if (tokenInfoJson != null && userInfoJson != null) {
                val tokenInfo = gson.fromJson(tokenInfoJson, TokenInfo::class.java)
                val userInfo = gson.fromJson(userInfoJson, AuthUser::class.java)
                
                _tokenInfo.value = tokenInfo
                _currentUser.value = userInfo
                
                // 检查Token是否过期
                _authStatus.value = if (tokenInfo.isExpired()) {
                    AuthStatus.EXPIRED
                } else {
                    AuthStatus.AUTHENTICATED
                }
            } else {
                _authStatus.value = AuthStatus.UNAUTHENTICATED
            }
            
        } catch (e: Exception) {
            _authStatus.value = AuthStatus.ERROR
        }
    }
    
    /**
     * 获取授权头
     */
    fun getAuthHeader(): String? {
        val token = getCurrentToken()
        return if (token != null) {
            "Bearer $token"
        } else {
            null
        }
    }
    
    /**
     * 清除所有认证数据
     */
    suspend fun clearAll() {
        logout()
    }
}