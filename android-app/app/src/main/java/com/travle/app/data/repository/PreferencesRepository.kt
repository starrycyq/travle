package com.travle.app.data.repository

import com.travle.app.data.api.TravelApiService
import com.travle.app.data.database.UserPreferenceDao
import com.travle.app.data.database.UserPreference
import com.travle.app.data.model.InputPreferenceRequest
import com.travle.app.data.model.InputPreferenceResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val apiService: TravelApiService,
    private val userPreferenceDao: UserPreferenceDao
) {
    suspend fun saveUserPreference(destination: String, preferences: String): Result<InputPreferenceResult> {
        return try {
            val request = InputPreferenceRequest(
                destination = destination,
                preferences = preferences
            )
            val result = apiService.inputPreference(request)
            if (result.status == "success") {
                // 同时保存到本地数据库
                val localPreference = UserPreference(
                    destination = destination,
                    preferences = preferences
                )
                userPreferenceDao.insertPreference(localPreference)
                
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "保存偏好失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserPreferences(): Flow<List<UserPreference>> {
        return userPreferenceDao.getAllPreferences()
    }

    fun searchUserPreferences(destination: String, preferences: String, limit: Int = 10): Flow<List<UserPreference>> {
        return userPreferenceDao.getPreferencesByQuery("%$destination%", "%$preferences%", limit)
    }

    suspend fun deleteUserPreference(userPreference: UserPreference) {
        userPreferenceDao.deletePreference(userPreference)
    }

    suspend fun clearAllUserPreferences() {
        userPreferenceDao.clearAllPreferences()
    }
    
    // TODO: 临时实现，需要替换为实际的数据存储
    private val _nickname = MutableStateFlow("旅行爱好者")
    val nickname: StateFlow<String> = _nickname
    
    private val _theme = MutableStateFlow("light")
    val theme: StateFlow<String> = _theme
    
    suspend fun setNickname(nickname: String) {
        _nickname.value = nickname
    }
    
    suspend fun setTheme(theme: String) {
        _theme.value = theme
    }
}