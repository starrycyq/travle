package com.travle.app.data.repository

import com.travle.app.data.api.TravelApiService
import com.travle.app.data.database.CollectionDao
import com.travle.app.data.database.CollectionItem
import com.travle.app.data.model.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class TravelRepositoryTest {
    
    private lateinit var repository: TravelRepository
    private lateinit var mockApiService: TravelApiService
    private lateinit var mockCollectionDao: CollectionDao
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private val testGuideRequest = GenerateGuideRequest(
        destination = "上海",
        preferences = "美食,文化"
    )
    
    private val testGuideResult = GuideResult(
        status = "success",
        guide = "上海旅行攻略...",
        images = listOf("img1.jpg", "img2.jpg"),
        message = null,
        context_length = 100,
        retrieved_docs = 5,
        model_info = ModelInfo(
            use_real_model = true,
            nlp_available = true,
            embedding_model_type = "bge"
        ),
        id = "guide_123"
    )
    
    private val testUploadRequest = UploadGuideRequest(
        text = "攻略内容",
        images = listOf("img1.jpg")
    )
    
    private val testInputPreferenceRequest = InputPreferenceRequest(
        destination = "北京",
        preferences = "历史,古迹"
    )
    
    private val testInputPreferenceResult = InputPreferenceResult(
        status = "success",
        message = "偏好记录成功"
    )
    
    private val testSearchRequest = SearchGuidesRequest(
        query = "上海美食",
        filters = mapOf("category" to "food"),
        limit = 10
    )
    
    private val testSearchResult = SearchGuidesResult(
        status = "success",
        data = SearchGuidesData(
            query = "上海美食",
            results = listOf(
                GuideSearchResult(
                    id = "1",
                    title = "上海美食攻略",
                    content = "上海美食介绍...",
                    score = 0.95,
                    metadata = GuideMetadata(
                        destination = "上海",
                        images = listOf("img1.jpg"),
                        author = "用户1"
                    )
                )
            )
        )
    )
    
    private val testXiaohongshuRequest = AuthenticateXiaohongshuRequest(
        auth_token = "test_token",
        user_id = "user_123",
        expires_in = 3600
    )
    
    private val testXiaohongshuResult = AuthenticateXiaohongshuResult(
        status = "success",
        message = "认证成功",
        error_code = null
    )
    
    private val testCommunityPosts = listOf(
        CommunityPost(
            id = 1,
            content = "测试帖子1",
            destination = "上海",
            like_count = 10,
            create_time = "2024-01-01",
            anonymous_id = "user1",
            images = listOf("img1.jpg")
        )
    )
    
    private val testApiResponse = ApiResponse(
        code = 200,
        msg = "成功",
        data = testCommunityPosts
    )
    
    private val testCollectionItem = CollectionItem(
        id = 1,
        title = "测试收藏",
        content = "测试内容",
        type = "guide",
        createdTime = System.currentTimeMillis()
    )
    
    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        
        mockApiService = mockk()
        mockCollectionDao = mockk()
        
        repository = TravelRepository(mockApiService, mockCollectionDao)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `generateGuide should return success on successful API call`() = testScope.runTest {
        // Given
        coEvery { mockApiService.generateGuide(testGuideRequest) } returns testGuideResult
        
        // When
        val result = repository.generateGuide(testGuideRequest)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testGuideResult, result.getOrNull())
        coVerify { mockApiService.generateGuide(testGuideRequest) }
    }
    
    @Test
    fun `generateGuide should return failure on API error`() = testScope.runTest {
        // Given
        val errorResult = testGuideResult.copy(status = "error", message = "生成失败")
        coEvery { mockApiService.generateGuide(testGuideRequest) } returns errorResult
        
        // When
        val result = repository.generateGuide(testGuideRequest)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("生成攻略失败", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `generateGuide should return failure on exception`() = testScope.runTest {
        // Given
        coEvery { mockApiService.generateGuide(testGuideRequest) } throws RuntimeException("网络错误")
        
        // When
        val result = repository.generateGuide(testGuideRequest)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("网络错误", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `uploadGuide should return success on successful API call`() = testScope.runTest {
        // Given
        coEvery { mockApiService.uploadGuide(testUploadRequest) } returns testGuideResult
        
        // When
        val result = repository.uploadGuide(testUploadRequest)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testGuideResult, result.getOrNull())
        coVerify { mockApiService.uploadGuide(testUploadRequest) }
    }
    
    @Test
    fun `uploadGuide should return failure on API error`() = testScope.runTest {
        // Given
        val errorResult = testGuideResult.copy(status = "error", message = "上传失败")
        coEvery { mockApiService.uploadGuide(testUploadRequest) } returns errorResult
        
        // When
        val result = repository.uploadGuide(testUploadRequest)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("上传攻略失败", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `inputPreference should return success on successful API call`() = testScope.runTest {
        // Given
        coEvery { mockApiService.inputPreference(testInputPreferenceRequest) } returns testInputPreferenceResult
        
        // When
        val result = repository.inputPreference(testInputPreferenceRequest)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testInputPreferenceResult, result.getOrNull())
        coVerify { mockApiService.inputPreference(testInputPreferenceRequest) }
    }
    
    @Test
    fun `inputPreference should return failure on API error`() = testScope.runTest {
        // Given
        val errorResult = testInputPreferenceResult.copy(status = "error", message = "记录失败")
        coEvery { mockApiService.inputPreference(testInputPreferenceRequest) } returns errorResult
        
        // When
        val result = repository.inputPreference(testInputPreferenceRequest)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("记录偏好失败", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `searchGuides should return success on successful API call`() = testScope.runTest {
        // Given
        coEvery { mockApiService.searchGuides(testSearchRequest) } returns testSearchResult
        
        // When
        val result = repository.searchGuides(testSearchRequest)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testSearchResult, result.getOrNull())
        coVerify { mockApiService.searchGuides(testSearchRequest) }
    }
    
    @Test
    fun `searchGuides should return failure on API error`() = testScope.runTest {
        // Given
        val errorResult = testSearchResult.copy(status = "error", message = "搜索失败")
        coEvery { mockApiService.searchGuides(testSearchRequest) } returns errorResult
        
        // When
        val result = repository.searchGuides(testSearchRequest)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("搜索攻略失败", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `authenticateXiaohongshu should return success on successful API call`() = testScope.runTest {
        // Given
        coEvery { mockApiService.authenticateXiaohongshu(testXiaohongshuRequest) } returns testXiaohongshuResult
        
        // When
        val result = repository.authenticateXiaohongshu(testXiaohongshuRequest)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testXiaohongshuResult, result.getOrNull())
        coVerify { mockApiService.authenticateXiaohongshu(testXiaohongshuRequest) }
    }
    
    @Test
    fun `authenticateXiaohongshu should return failure on API error`() = testScope.runTest {
        // Given
        val errorResult = testXiaohongshuResult.copy(status = "error", message = "认证失败")
        coEvery { mockApiService.authenticateXiaohongshu(testXiaohongshuRequest) } returns errorResult
        
        // When
        val result = repository.authenticateXiaohongshu(testXiaohongshuRequest)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("小红书认证失败", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `getCommunityList should return success on successful API call`() = testScope.runTest {
        // Given
        coEvery { mockApiService.getCommunityList() } returns testApiResponse
        
        // When
        val result = repository.getCommunityList()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testCommunityPosts, result.getOrNull())
        coVerify { mockApiService.getCommunityList() }
    }
    
    @Test
    fun `getCommunityList should return failure on non-200 response`() = testScope.runTest {
        // Given
        val errorResponse = testApiResponse.copy(code = 500, msg = "服务器错误", data = null)
        coEvery { mockApiService.getCommunityList() } returns errorResponse
        
        // When
        val result = repository.getCommunityList()
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("获取列表失败", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `getCommunityList should return empty list on null data`() = testScope.runTest {
        // Given
        val response = testApiResponse.copy(data = null)
        coEvery { mockApiService.getCommunityList() } returns response
        
        // When
        val result = repository.getCommunityList()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrNull())
    }
    
    @Test
    fun `publishPost should return success on successful API call`() = testScope.runTest {
        // Given
        val request = PublishRequest(
            content = "测试内容",
            destination = "上海",
            images = listOf("img1.jpg")
        )
        val response = ApiResponse<Map<String, Any>>(code = 200, msg = "成功", data = mapOf("post_id" to 1))
        coEvery { mockApiService.publishPost(request) } returns response
        
        // When
        val result = repository.publishPost(request)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApiService.publishPost(request) }
    }
    
    @Test
    fun `publishPost should return failure on non-200 response`() = testScope.runTest {
        // Given
        val request = PublishRequest(
            content = "测试内容",
            destination = "上海"
        )
        val response = ApiResponse<Map<String, Any>>(code = 400, msg = "请求错误", data = null)
        coEvery { mockApiService.publishPost(request) } returns response
        
        // When
        val result = repository.publishPost(request)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("发布失败", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `likePost should return success on successful API call`() = testScope.runTest {
        // Given
        val postId = 1
        val response = ApiResponse<Unit>(code = 200, msg = "成功", data = Unit)
        coEvery { mockApiService.likePost(LikeRequest(postId)) } returns response
        
        // When
        val result = repository.likePost(postId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { mockApiService.likePost(LikeRequest(postId)) }
    }
    
    @Test
    fun `likePost should return failure on non-200 response`() = testScope.runTest {
        // Given
        val postId = 1
        val response = ApiResponse<Unit>(code = 404, msg = "帖子不存在", data = Unit)
        coEvery { mockApiService.likePost(LikeRequest(postId)) } returns response
        
        // When
        val result = repository.likePost(postId)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("点赞失败", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `getAllCollections should return flow from dao`() = testScope.runTest {
        // Given
        val collections = listOf(testCollectionItem)
        every { mockCollectionDao.getAllCollections() } returns flowOf(collections)
        
        // When
        val result = repository.getAllCollections()
        
        // Then
        val collected = result.toList(testScope.coroutineContext)
        assertEquals(listOf(collections), collected)
        verify { mockCollectionDao.getAllCollections() }
    }
    
    @Test
    fun `addCollection should call dao insert`() = testScope.runTest {
        // Given
        coEvery { mockCollectionDao.insertCollection(testCollectionItem) } just Runs
        
        // When
        repository.addCollection(testCollectionItem)
        
        // Then
        coVerify { mockCollectionDao.insertCollection(testCollectionItem) }
    }
    
    @Test
    fun `deleteCollection should call dao delete`() = testScope.runTest {
        // Given
        coEvery { mockCollectionDao.deleteCollection(testCollectionItem) } just Runs
        
        // When
        repository.deleteCollection(testCollectionItem)
        
        // Then
        coVerify { mockCollectionDao.deleteCollection(testCollectionItem) }
    }
    
    @Test
    fun `clearAllCollections should call dao clear`() = testScope.runTest {
        // Given
        coEvery { mockCollectionDao.clearAllCollections() } just Runs
        
        // When
        repository.clearAllCollections()
        
        // Then
        coVerify { mockCollectionDao.clearAllCollections() }
    }
    
    @Test
    fun `repository should handle all API methods correctly`() = testScope.runTest {
        // Test all API methods in one comprehensive test
        val methods = listOf(
            { repository.generateGuide(testGuideRequest) },
            { repository.uploadGuide(testUploadRequest) },
            { repository.inputPreference(testInputPreferenceRequest) },
            { repository.searchGuides(testSearchRequest) },
            { repository.authenticateXiaohongshu(testXiaohongshuRequest) },
            { repository.getCommunityList() },
            { repository.publishPost(PublishRequest(content = "test")) },
            { repository.likePost(1) }
        )
        
        // Setup successful responses for all
        coEvery { mockApiService.generateGuide(any()) } returns testGuideResult
        coEvery { mockApiService.uploadGuide(any()) } returns testGuideResult
        coEvery { mockApiService.inputPreference(any()) } returns testInputPreferenceResult
        coEvery { mockApiService.searchGuides(any()) } returns testSearchResult
        coEvery { mockApiService.authenticateXiaohongshu(any()) } returns testXiaohongshuResult
        coEvery { mockApiService.getCommunityList() } returns testApiResponse
        coEvery { mockApiService.publishPost(any()) } returns ApiResponse(200, "成功", mapOf())
        coEvery { mockApiService.likePost(any()) } returns ApiResponse(200, "成功", Unit)
        
        // Execute all methods
        methods.forEach { method ->
            val result = method()
            assertTrue(result.isSuccess, "Method should succeed")
        }
        
        // Verify all were called
        coVerify {
            mockApiService.generateGuide(any())
            mockApiService.uploadGuide(any())
            mockApiService.inputPreference(any())
            mockApiService.searchGuides(any())
            mockApiService.authenticateXiaohongshu(any())
            mockApiService.getCommunityList()
            mockApiService.publishPost(any())
            mockApiService.likePost(any())
        }
    }
}