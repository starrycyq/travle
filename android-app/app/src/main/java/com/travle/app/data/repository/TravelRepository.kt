package com.travle.app.data.repository

import com.travle.app.data.api.TravelApiService
import com.travle.app.data.database.CollectionDao
import com.travle.app.data.database.CollectionItem
import com.travle.app.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravelRepository @Inject constructor(
    private val apiService: TravelApiService,
    private val collectionDao: CollectionDao
) {
    // API调用
    suspend fun generateGuide(request: GenerateGuideRequest): Result<GuideResult> {
        return try {
            val result = apiService.generateGuide(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "生成攻略失败"))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true -> "无法连接到服务器，请检查网络连接或服务器地址"
                e.message?.contains("timeout") == true -> "请求超时，请检查网络连接"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址，请检查API配置"
                else -> "网络错误：${e.message ?: "未知错误"}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun uploadGuide(request: UploadGuideRequest): Result<GuideResult> {
        return try {
            val result = apiService.uploadGuide(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "上传攻略失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inputPreference(request: InputPreferenceRequest): Result<InputPreferenceResult> {
        return try {
            val result = apiService.inputPreference(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "记录偏好失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchGuides(request: SearchGuidesRequest): Result<SearchGuidesResult> {
        return try {
            val result = apiService.searchGuides(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception("搜索攻略失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun authenticateXiaohongshu(request: AuthenticateXiaohongshuRequest): Result<AuthenticateXiaohongshuResult> {
        return try {
            val result = apiService.authenticateXiaohongshu(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "小红书认证失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommunityList(): Result<List<CommunityPost>> {
        return try {
            val response = apiService.getCommunityList()
            if (response.code == 200) {
                Result.success(response.data ?: emptyList())
            } else {
                Result.failure(Exception(response.msg ?: "获取列表失败"))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true -> "无法连接到服务器，请检查网络连接或服务器地址"
                e.message?.contains("timeout") == true -> "请求超时，请检查网络连接"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址，请检查API配置"
                else -> "网络错误：${e.message ?: "未知错误"}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun publishPost(request: PublishRequest): Result<Unit> {
        return try {
            val response = apiService.publishPost(request)
            if (response.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.msg ?: "发布失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 用户认证
    suspend fun login(request: com.travle.app.data.model.LoginRequest): Result<com.travle.app.data.model.LoginResult> {
        return try {
            val result = apiService.login(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "登录失败"))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true -> "无法连接到服务器，请检查网络连接或服务器地址"
                e.message?.contains("timeout") == true -> "请求超时，请检查网络连接"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址，请检查API配置"
                else -> "登录失败：${e.message ?: "未知错误"}"
            }
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun register(request: com.travle.app.data.model.RegisterRequest): Result<com.travle.app.data.model.RegisterResult> {
        return try {
            val result = apiService.register(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "注册失败"))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true -> "无法连接到服务器，请检查网络连接或服务器地址"
                e.message?.contains("timeout") == true -> "请求超时，请检查网络连接"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址，请检查API配置"
                else -> "注册失败：${e.message ?: "未知错误"}"
            }
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getCurrentUser(): Result<com.travle.app.data.model.CurrentUserResult> {
        return try {
            val result = apiService.getCurrentUser()
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "获取用户信息失败"))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true -> "无法连接到服务器，请检查网络连接或服务器地址"
                e.message?.contains("timeout") == true -> "请求超时，请检查网络连接"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址，请检查API配置"
                else -> "获取用户信息失败：${e.message ?: "未知错误"}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun likePost(postId: Int): Result<Unit> {
        return try {
            val response = apiService.likePost(LikeRequest(postId))
            if (response.code == 200) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.msg ?: "点赞失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 本地收藏
    fun getAllCollections(): Flow<List<CollectionItem>> {
        return collectionDao.getAllCollections()
    }

    suspend fun addCollection(item: CollectionItem) {
        collectionDao.insertCollection(item)
    }

    suspend fun deleteCollection(item: CollectionItem) {
        collectionDao.deleteCollection(item)
    }

    suspend fun clearAllCollections() {
        collectionDao.clearAllCollections()
    }

    // AI聊天功能
    suspend fun sendChatMessage(message: String, conversationId: String? = null): Result<ChatResponse> {
        return try {
            val request = ChatRequest(message = message, conversation_id = conversationId)
            val result = apiService.sendChatMessage(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "发送消息失败"))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true -> "无法连接到服务器，请检查网络连接或服务器地址"
                e.message?.contains("timeout") == true -> "请求超时，请检查网络连接"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址，请检查API配置"
                else -> "发送消息失败：${e.message ?: "未知错误"}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun getChatHistory(conversationId: String? = null): Result<ChatHistoryResponse> {
        return try {
            val request = ChatHistoryRequest(conversation_id = conversationId)
            val result = apiService.getChatHistory(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "获取聊天历史失败"))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true -> "无法连接到服务器，请检查网络连接或服务器地址"
                e.message?.contains("timeout") == true -> "请求超时，请检查网络连接"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址，请检查API配置"
                else -> "获取聊天历史失败：${e.message ?: "未知错误"}"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    // 自驾游功能
    suspend fun generateRoadTrip(start: String, destination: String, preferences: String = "", routeType: String = "fastest"): Result<RoadTripResult> {
        return try {
            val request = RoadTripRequest(
                start = start,
                destination = destination,
                preferences = preferences,
                route_type = routeType
            )
            val result = apiService.generateRoadTrip(request)
            if (result.status == "success") {
                Result.success(result)
            } else {
                Result.failure(Exception(result.message ?: "生成自驾路线失败"))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true -> "无法连接到服务器，请检查网络连接或服务器地址"
                e.message?.contains("timeout") == true -> "请求超时，请检查网络连接"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址，请检查API配置"
                else -> "生成自驾路线失败：${e.message ?: "未知错误"}"
            }
            Result.failure(Exception(errorMessage))
        }
    }
}