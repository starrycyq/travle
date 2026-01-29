package com.travle.app.data.repository

import com.travle.app.data.api.TravelApiService
import com.travle.app.data.database.UserPreferenceDao
import com.travle.app.data.database.UserPreference
import com.travle.app.data.model.InputPreferenceRequest
import com.travle.app.data.model.InputPreferenceResult
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
class PreferencesRepositoryTest {
    
    private lateinit var repository: PreferencesRepository
    private lateinit var mockApiService: TravelApiService
    private lateinit var mockUserPreferenceDao: UserPreferenceDao
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private val testDestination = "上海"
    private val testPreferences = "美食,文化"
    
    private val testInputPreferenceResult = InputPreferenceResult(
        status = "success",
        message = "偏好记录成功"
    )
    
    private val testUserPreference = UserPreference(
        id = 1,
        destination = testDestination,
        preferences = testPreferences
    )
    
    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        
        mockApiService = mockk()
        mockUserPreferenceDao = mockk()
        
        repository = PreferencesRepository(mockApiService, mockUserPreferenceDao)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `saveUserPreference should call API and save to local database on success`() = testScope.runTest {
        // Given
        val request = InputPreferenceRequest(testDestination, testPreferences)
        coEvery { mockApiService.inputPreference(request) } returns testInputPreferenceResult
        coEvery { mockUserPreferenceDao.insertPreference(any()) } just Runs
        
        // When
        val result = repository.saveUserPreference(testDestination, testPreferences)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testInputPreferenceResult, result.getOrNull())
        
        coVerify {
            mockApiService.inputPreference(request)
            mockUserPreferenceDao.insertPreference(any())
        }
    }
    
    @Test
    fun `saveUserPreference should return failure on API error`() = testScope.runTest {
        // Given
        val request = InputPreferenceRequest(testDestination, testPreferences)
        val errorResult = InputPreferenceResult(status = "error", message = "保存失败")
        coEvery { mockApiService.inputPreference(request) } returns errorResult
        
        // When
        val result = repository.saveUserPreference(testDestination, testPreferences)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("保存偏好失败", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 0) { mockUserPreferenceDao.insertPreference(any()) }
    }
    
    @Test
    fun `saveUserPreference should return failure on exception`() = testScope.runTest {
        // Given
        val request = InputPreferenceRequest(testDestination, testPreferences)
        coEvery { mockApiService.inputPreference(request) } throws RuntimeException("网络错误")
        
        // When
        val result = repository.saveUserPreference(testDestination, testPreferences)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("网络错误", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `getUserPreferences should return flow from dao`() = testScope.runTest {
        // Given
        val preferences = listOf(testUserPreference)
        every { mockUserPreferenceDao.getAllPreferences() } returns flowOf(preferences)
        
        // When
        val result = repository.getUserPreferences()
        
        // Then
        val collected = result.toList(testScope.coroutineContext)
        assertEquals(listOf(preferences), collected)
        verify { mockUserPreferenceDao.getAllPreferences() }
    }
    
    @Test
    fun `searchUserPreferences should call dao with correct parameters`() = testScope.runTest {
        // Given
        val query = "上海"
        val prefQuery = "美食"
        val limit = 5
        val preferences = listOf(testUserPreference)
        every { mockUserPreferenceDao.getPreferencesByQuery("%$query%", "%$prefQuery%", limit) } returns flowOf(preferences)
        
        // When
        val result = repository.searchUserPreferences(query, prefQuery, limit)
        
        // Then
        val collected = result.toList(testScope.coroutineContext)
        assertEquals(listOf(preferences), collected)
        verify { mockUserPreferenceDao.getPreferencesByQuery("%$query%", "%$prefQuery%", limit) }
    }
    
    @Test
    fun `deleteUserPreference should call dao delete`() = testScope.runTest {
        // Given
        coEvery { mockUserPreferenceDao.deletePreference(testUserPreference) } just Runs
        
        // When
        repository.deleteUserPreference(testUserPreference)
        
        // Then
        coVerify { mockUserPreferenceDao.deletePreference(testUserPreference) }
    }
    
    @Test
    fun `clearAllUserPreferences should call dao clear`() = testScope.runTest {
        // Given
        coEvery { mockUserPreferenceDao.clearAllPreferences() } just Runs
        
        // When
        repository.clearAllUserPreferences()
        
        // Then
        coVerify { mockUserPreferenceDao.clearAllPreferences() }
    }
    
    @Test
    fun `nickname flow should provide default value`() = testScope.runTest {
        // Given
        val expectedNickname = "旅行爱好者"
        
        // When
        val nicknameFlow = repository.nickname
        
        // Then
        val nickname = nicknameFlow.toList(testScope.coroutineContext).first()
        assertEquals(expectedNickname, nickname)
    }
    
    @Test
    fun `setNickname should update nickname flow`() = testScope.runTest {
        // Given
        val newNickname = "新昵称"
        
        // When
        repository.setNickname(newNickname)
        
        // Then
        val nicknameFlow = repository.nickname
        val nickname = nicknameFlow.toList(testScope.coroutineContext).first()
        assertEquals(newNickname, nickname)
    }
    
    @Test
    fun `theme flow should provide default value`() = testScope.runTest {
        // Given
        val expectedTheme = "light"
        
        // When
        val themeFlow = repository.theme
        
        // Then
        val theme = themeFlow.toList(testScope.coroutineContext).first()
        assertEquals(expectedTheme, theme)
    }
    
    @Test
    fun `setTheme should update theme flow`() = testScope.runTest {
        // Given
        val newTheme = "dark"
        
        // When
        repository.setTheme(newTheme)
        
        // Then
        val themeFlow = repository.theme
        val theme = themeFlow.toList(testScope.coroutineContext).first()
        assertEquals(newTheme, theme)
    }
    
    @Test
    fun `repository should handle multiple operations correctly`() = testScope.runTest {
        // Test comprehensive usage
        val testNickname = "测试用户"
        val testTheme = "dark"
        
        // Set nickname and theme
        repository.setNickname(testNickname)
        repository.setTheme(testTheme)
        
        // Verify flows
        val nickname = repository.nickname.toList(testScope.coroutineContext).first()
        val theme = repository.theme.toList(testScope.coroutineContext).first()
        
        assertEquals(testNickname, nickname)
        assertEquals(testTheme, theme)
        
        // Test preference saving
        coEvery { mockApiService.inputPreference(any()) } returns testInputPreferenceResult
        coEvery { mockUserPreferenceDao.insertPreference(any()) } just Runs
        
        val saveResult = repository.saveUserPreference("北京", "历史")
        assertTrue(saveResult.isSuccess)
        
        // Test getting preferences
        val preferences = listOf(testUserPreference)
        every { mockUserPreferenceDao.getAllPreferences() } returns flowOf(preferences)
        val prefFlow = repository.getUserPreferences()
        val collected = prefFlow.toList(testScope.coroutineContext).first()
        assertEquals(preferences, collected)
    }
}