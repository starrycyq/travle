package com.travle.app.data.api

import com.travle.app.data.model.*
import retrofit2.http.*

interface TravelApiService {
    // 系统信息
    @GET("/")
    suspend fun getSystemInfo(): SystemInfoResult
    
    @GET("/model-info")
    suspend fun getModelInfo(): ModelInfoResult

    // 认证功能
    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResult
    
    @POST("/api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResult
    
    @POST("/api/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): RefreshTokenResult
    
    @POST("/api/auth/verify")
    suspend fun verifyToken(
        @Body request: VerifyTokenRequest
    ): VerifyTokenResult
    
    @GET("/api/auth/me")
    suspend fun getCurrentUser(): CurrentUserResult
    
    // 偏好管理
    @POST("/input-preference")
    suspend fun inputPreference(
        @Body request: InputPreferenceRequest
    ): InputPreferenceResult

    // 攻略生成
    @POST("/generate-guide")
    suspend fun generateGuide(
        @Body request: GenerateGuideRequest
    ): GuideResult

    @POST("/upload-guide")
    suspend fun uploadGuide(
        @Body request: UploadGuideRequest
    ): GuideResult

    // 社区功能
    @GET("/community/list")
    suspend fun getCommunityList(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<List<CommunityPost>>

    @POST("/community/publish")
    suspend fun publishPost(
        @Body request: PublishRequest
    ): ApiResponse<Map<String, Any>>
    
    @POST("/community/like")
    suspend fun likePost(
        @Body request: LikeRequest
    ): ApiResponse<Unit>
    
    // 评论功能
    @GET("/community/{postId}/comments")
    suspend fun getComments(
        @Path("postId") postId: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<List<Comment>>
    
    @POST("/community/{postId}/comments")
    suspend fun addComment(
        @Path("postId") postId: Int,
        @Body request: AddCommentRequest
    ): ApiResponse<Map<String, Any>>
    
    @POST("/community/comments/{commentId}/like")
    suspend fun likeComment(
        @Path("commentId") commentId: Int
    ): ApiResponse<Unit>

    // 认证功能
    @POST("/api/auth/xiaohongshu")
    suspend fun xiaohongshuAuth(
        @Body request: XiaohongshuAuthRequest
    ): ApiResponse<Unit>
    
    @POST("/api/auth/xiaohongshu")
    suspend fun authenticateXiaohongshu(
        @Body request: AuthenticateXiaohongshuRequest
    ): AuthenticateXiaohongshuResult

    // 搜索功能
    @POST("/api/search/guides")
    suspend fun searchGuides(
        @Body request: SearchGuidesRequest
    ): SearchGuidesResult
    
    // 健康检查
    @GET("/health")
    suspend fun healthCheck(): HealthCheckResult

    // AI聊天功能
    @POST("/api/chat")
    suspend fun sendChatMessage(
        @Body request: ChatRequest
    ): ChatResponse

    @POST("/api/chat/history")
    suspend fun getChatHistory(
        @Body request: ChatHistoryRequest
    ): ChatHistoryResponse

    // 自驾游功能
    @POST("/api/roadtrip")
    suspend fun generateRoadTrip(
        @Body request: RoadTripRequest
    ): RoadTripResult
}