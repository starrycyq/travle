package com.travle.app.data.repository

import com.travle.app.data.api.TravelApiService
import com.travle.app.data.model.InputPreferenceRequest
import com.travle.app.data.model.InputPreferenceResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val apiService: TravelApiService
) {
    suspend fun saveUserPreference(destination: String, preferences: String): Result<InputPreferenceResult> {
        return try {
            val request = InputPreferenceRequest(
                destination = destination,
                preferences = preferences
            )
            val result = apiService.inputPreference(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "保存偏好失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}