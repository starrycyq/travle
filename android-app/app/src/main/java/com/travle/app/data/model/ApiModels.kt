package com.travle.app.data.model

data class GenerateGuideRequest(
    val destination: String,
    val preferences: String
)



data class UploadGuideRequest(
    val text: String,
    val images: List<String> = emptyList()
)

data class InputPreferenceRequest(
    val destination: String,
    val preferences: String
)

data class InputPreferenceResult(
    val status: String,
    val message: String? = null
)

data class SearchGuidesRequest(
    val query: String,
    val filters: Map<String, String> = mapOf(),
    val limit: Int = 5
)

data class SearchGuidesResult(
    val status: String,
    val data: SearchGuidesData? = null
)

data class SearchGuidesData(
    val query: String,
    val results: List<GuideSearchResult>
)

data class GuideSearchResult(
    val id: String,
    val title: String,
    val content: String,
    val score: Double,
    val metadata: GuideMetadata
)

data class GuideMetadata(
    val destination: String,
    val images: List<String>,
    val author: String
)

data class AuthenticateXiaohongshuRequest(
    val auth_token: String,
    val user_id: String,
    val expires_in: Int = 3600
)

data class AuthenticateXiaohongshuResult(
    val status: String,
    val message: String? = null,
    val error_code: String? = null
)

data class CommunityPost(
    val id: Int,
    val content: String,
    val destination: String? = null,
    val like_count: Int = 0,
    val create_time: String = "",
    val anonymous_id: String? = null,
    val images: List<String>? = null
)

data class ApiResponse<T>(
    val code: Int,
    val msg: String? = null,
    val data: T? = null
)

data class LikeRequest(
    val post_id: Int
)

data class PublishRequest(
    val content: String,
    val destination: String? = null,
    val images: List<String> = emptyList()
)

// 认证相关数据模型
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResult(
    val status: String,
    val message: String? = null,
    val data: LoginData? = null
)

data class LoginData(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val user_info: UserInfo
)

data class UserInfo(
    val user_id: String,
    val username: String,
    val email: String,
    val role: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null
)

data class RegisterResult(
    val status: String,
    val message: String? = null,
    val data: RegisterData? = null
)

data class RegisterData(
    val user_id: String,
    val username: String,
    val email: String
)

data class RefreshTokenRequest(
    // 使用当前token，不需要额外参数
    val dummy: String? = null
)

data class RefreshTokenResult(
    val status: String,
    val message: String? = null,
    val data: RefreshTokenData? = null
)

data class RefreshTokenData(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)

data class VerifyTokenRequest(
    val token: String
)

data class VerifyTokenResult(
    val status: String,
    val message: String? = null,
    val data: VerifyTokenData? = null
)

data class VerifyTokenData(
    val user_id: String,
    val user_data: Map<String, Any>,
    val expires_at: Long
)

data class CurrentUserResult(
    val status: String,
    val message: String? = null,
    val data: CurrentUserData? = null
)

data class CurrentUserData(
    val user_id: String,
    val user_data: Map<String, Any>
)

// 系统信息相关
data class SystemInfoResult(
    val status: String,
    val version: String? = null,
    val apis: List<ApiInfo>,
    val authentication: AuthInfo? = null
)

data class ApiInfo(
    val path: String,
    val method: String,
    val description: String
)

data class AuthInfo(
    val type: String,
    val login_endpoint: String,
    val token_refresh: String,
    val token_verify: String
)

data class ModelInfoResult(
    val status: String,
    val model_info: ModelInfo
)

data class ModelInfo(
    val use_real_model: Boolean,
    val nlp_available: Boolean,
    val embedding_model_type: String
)

// 健康检查相关
data class HealthCheckResult(
    val status: String,
    val timestamp: String,
    val services: HealthServices,
    val authenticated_user: CurrentUserData? = null
)

data class HealthServices(
    val database: String,
    val model_service: ModelInfo,
    val authentication: String
)

// 评论相关
data class Comment(
    val id: Int,
    val post_id: Int,
    val content: String,
    val author_name: String = "匿名用户",
    val create_time: String = "",
    val like_count: Int = 0
)

data class AddCommentRequest(
    val content: String,
    val author_name: String = "匿名用户"
)

// 修正命名不一致的问题
data class XiaohongshuAuthRequest(
    val auth_token: String,
    val user_id: String,
    val expires_in: Int = 3600
)

data class SearchResult(
    val query: String,
    val results: List<GuideSearchResult> = emptyList()
)

// 修正GuideResult，包含新字段
data class GuideResult(
    val status: String,
    val guide: String? = null,
    val images: List<String>? = null,
    val message: String? = null,
    val context_length: Int? = null,
    val retrieved_docs: Int? = null,
    val model_info: ModelInfo? = null,
    val id: String? = null
)

// 自驾游相关数据模型
data class RoadTripRequest(
    val start: String,
    val destination: String,
    val preferences: String = "",
    val route_type: String = "fastest" // "fastest", "scenic", "balanced"
)

data class RoadTripResult(
    val status: String,
    val route: RoadTripRoute? = null,
    val guide: String? = null,
    val images: List<String>? = null,
    val message: String? = null,
    val distance_km: Double? = null,
    val estimated_hours: Double? = null,
    val waypoints: List<Waypoint>? = null
)

data class RoadTripRoute(
    val start: String,
    val destination: String,
    val waypoints: List<Waypoint>,
    val total_distance_km: Double,
    val estimated_time_hours: Double,
    val route_description: String
)

data class Waypoint(
    val name: String,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val stay_time_minutes: Int = 0
)

// AI聊天相关数据模型
data class ChatMessage(
    val id: String,
    val role: String, // "user" 或 "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatRequest(
    val message: String,
    val conversation_id: String? = null,
    val context: Map<String, Any>? = null
)

data class ChatResponse(
    val status: String,
    val message: String? = null,
    val data: ChatResponseData? = null
)

data class ChatResponseData(
    val response: String,
    val conversation_id: String? = null,
    val context: Map<String, Any>? = null
)

data class ChatHistoryRequest(
    val conversation_id: String? = null,
    val limit: Int = 50
)

data class ChatHistoryResponse(
    val status: String,
    val message: String? = null,
    val data: ChatHistoryData? = null
)

data class ChatHistoryData(
    val messages: List<ChatMessageData>,
    val conversation_id: String? = null
)

data class ChatMessageData(
    val role: String,
    val content: String,
    val timestamp: Long
)